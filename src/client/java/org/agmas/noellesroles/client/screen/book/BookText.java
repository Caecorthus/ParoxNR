package org.agmas.noellesroles.client.screen.book;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CJK-aware line breaking for the reader pages: no line starts with a closing
 * mark, no line ends with an opening mark, widow avoidance and balanced wraps.
 * Cells carry their own style so keywords can be tinted mid-line.
 */
public final class BookText {
    static final String CLOSE = "，。、；：？！）〕」』》】…—,.;:?!)]％”’";
    static final String OPEN = "（〔「『《【([“‘";
    private static final String WORD_SYMBOLS = "+-.%/×÷_'#";
    private static final int BRACKET_SPAN = 8;
    private static final Pattern EMPHASIS = Pattern.compile("【[^】]*】|\\d+ ?金币");

    private BookText() {
    }

    public record Cell(int codePoint, Style style) {
    }

    public static Style ink(int argb) {
        return Style.EMPTY.withColor(argb & 0xFFFFFF);
    }

    public static List<Cell> cells(String text, int argb) {
        Style style = ink(argb);
        List<Cell> out = new ArrayList<>(text.length());
        text.codePoints().forEach(cp -> out.add(new Cell(cp, style)));
        return out;
    }

    public static List<Cell> cells(Text text, int fallbackArgb) {
        List<Cell> out = new ArrayList<>();
        text.visit((style, string) -> {
            Style resolved = style.getColor() != null ? style : style.withColor(fallbackArgb & 0xFFFFFF);
            string.codePoints().forEach(cp -> out.add(new Cell(cp, resolved)));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /** Cells with 【keywords】 in the item colour and coin amounts in gold. */
    public static List<Cell> emphasised(Text text, int baseArgb, int itemArgb, int moneyArgb) {
        List<Cell> out = new ArrayList<>();
        text.visit((style, string) -> {
            if (style.getColor() != null) {
                string.codePoints().forEach(cp -> out.add(new Cell(cp, style)));
                return Optional.empty();
            }
            Style base = style.withColor(baseArgb & 0xFFFFFF);
            Style item = style.withColor(itemArgb & 0xFFFFFF);
            Style money = style.withColor(moneyArgb & 0xFFFFFF);
            Matcher matcher = EMPHASIS.matcher(string);
            int last = 0;
            while (matcher.find()) {
                appendCells(out, string.substring(last, matcher.start()), base);
                appendCells(out, matcher.group(), matcher.group().startsWith("【") ? item : money);
                last = matcher.end();
            }
            appendCells(out, string.substring(last), base);
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static void appendCells(List<Cell> out, String text, Style style) {
        text.codePoints().forEach(cp -> out.add(new Cell(cp, style)));
    }

    /** Removes spaces that sit between CJK text and punctuation or digits. */
    public static List<Cell> normalise(List<Cell> cells) {
        List<Cell> out = new ArrayList<>(cells.size());
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i).codePoint() == 32 && i > 0 && i + 1 < cells.size()) {
                int previous = cells.get(i - 1).codePoint();
                int next = cells.get(i + 1).codePoint();
                if ((digit(previous) && ideograph(next)) || fullWidthPunctuation(previous) || fullWidthPunctuation(next)) {
                    continue;
                }
            }
            out.add(cells.get(i));
        }
        return out;
    }

    public static List<List<Cell>> wrap(List<Cell> cells, int maxWidth, TextRenderer font, boolean avoidWidow) {
        List<List<List<Cell>>> lines = new ArrayList<>();
        List<List<Cell>> current = new ArrayList<>();
        int currentWidth = 0;
        int paragraphStart = 0;
        for (List<Cell> token : tokens(cells)) {
            int first = token.get(0).codePoint();
            if (first == 10) {
                lines.add(current);
                if (avoidWidow) {
                    avoidWidow(lines, paragraphStart, maxWidth, font);
                }
                paragraphStart = lines.size();
                current = new ArrayList<>();
                currentWidth = 0;
                continue;
            }
            int tokenWidth = width(token, font);
            if (first == 32) {
                if (current.isEmpty()) {
                    continue;
                }
                current.add(token);
                currentWidth += tokenWidth;
                continue;
            }
            if (currentWidth + tokenWidth > maxWidth && !current.isEmpty()) {
                lines.add(current);
                current = new ArrayList<>();
                currentWidth = 0;
            }
            if (tokenWidth > maxWidth) {
                for (Cell cell : token) {
                    int glyph = advance(font, cell);
                    if (currentWidth + glyph > maxWidth && !current.isEmpty()) {
                        lines.add(current);
                        current = new ArrayList<>();
                        currentWidth = 0;
                    }
                    current.add(List.of(cell));
                    currentWidth += glyph;
                }
                continue;
            }
            current.add(token);
            currentWidth += tokenWidth;
        }
        lines.add(current);
        if (avoidWidow) {
            avoidWidow(lines, paragraphStart, maxWidth, font);
        }
        List<List<Cell>> out = new ArrayList<>(lines.size());
        for (List<List<Cell>> line : lines) {
            List<Cell> flat = new ArrayList<>();
            line.forEach(flat::addAll);
            out.add(trim(flat));
        }
        return out;
    }

    private static void avoidWidow(List<List<List<Cell>>> lines, int from, int maxWidth, TextRenderer font) {
        int last = lines.size() - 1;
        if (last - 1 < from) {
            return;
        }
        List<List<Cell>> tail = lines.get(last);
        if (glyphs(tail) != 1) {
            return;
        }
        List<List<Cell>> previous = lines.get(last - 1);
        int end = previous.size();
        while (end > 0 && previous.get(end - 1).get(0).codePoint() == 32) {
            end--;
        }
        if (end == 0) {
            return;
        }
        List<List<Cell>> moved = new ArrayList<>(previous.subList(end - 1, previous.size()));
        List<List<Cell>> kept = new ArrayList<>(previous.subList(0, end - 1));
        int movedWidth = 0;
        for (List<Cell> token : moved) {
            movedWidth += width(token, font);
        }
        for (List<Cell> token : tail) {
            movedWidth += width(token, font);
        }
        if (glyphs(kept) < 2 || movedWidth > maxWidth) {
            return;
        }
        moved.addAll(tail);
        lines.set(last - 1, kept);
        lines.set(last, moved);
    }

    private static int glyphs(List<List<Cell>> tokens) {
        int count = 0;
        for (List<Cell> token : tokens) {
            for (Cell cell : token) {
                if (cell.codePoint() == 32 || CLOSE.indexOf(cell.codePoint()) >= 0) {
                    continue;
                }
                count++;
            }
        }
        return count;
    }

    public static OrderedText toOrderedText(List<Cell> line) {
        if (line.isEmpty()) {
            return OrderedText.EMPTY;
        }
        List<OrderedText> runs = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        Style style = line.get(0).style();
        for (Cell cell : line) {
            if (!cell.style().equals(style)) {
                runs.add(OrderedText.styledForwardsVisitedString(run.toString(), style));
                run.setLength(0);
                style = cell.style();
            }
            run.appendCodePoint(cell.codePoint());
        }
        runs.add(OrderedText.styledForwardsVisitedString(run.toString(), style));
        return OrderedText.concat(runs);
    }

    public static int width(List<Cell> cells, TextRenderer font) {
        int width = 0;
        for (Cell cell : cells) {
            width += advance(font, cell);
        }
        return width;
    }

    public static int advance(TextRenderer font, Cell cell) {
        return font.getWidth(OrderedText.styled(cell.codePoint(), cell.style()));
    }

    public static String string(List<Cell> cells) {
        StringBuilder out = new StringBuilder(cells.size());
        cells.forEach(cell -> out.appendCodePoint(cell.codePoint()));
        return out.toString();
    }

    private static List<List<Cell>> tokens(List<Cell> cells) {
        List<List<Cell>> tokens = new ArrayList<>();
        int i = 0;
        int n = cells.size();
        while (i < n) {
            int codePoint = cells.get(i).codePoint();
            if (codePoint == 32 || codePoint == 10) {
                tokens.add(List.of(cells.get(i++)));
                continue;
            }
            List<Cell> token = new ArrayList<>();
            while (i < n && OPEN.indexOf(cells.get(i).codePoint()) >= 0) {
                token.add(cells.get(i++));
            }
            boolean bracket = !token.isEmpty() && token.get(token.size() - 1).codePoint() == '【';
            int close = -1;
            if (bracket) {
                for (int j = i; j < n && j <= i + BRACKET_SPAN; j++) {
                    if (cells.get(j).codePoint() == '】') {
                        close = j;
                        break;
                    }
                }
            }
            if (close >= 0) {
                while (i <= close) {
                    token.add(cells.get(i++));
                }
            } else if (i < n && word(cells.get(i).codePoint())) {
                boolean number = false;
                while (i < n && word(cells.get(i).codePoint())) {
                    number |= digit(cells.get(i).codePoint());
                    token.add(cells.get(i++));
                }
                if (number) {
                    int j = i;
                    if (j < n && cells.get(j).codePoint() == 32) {
                        j++;
                    }
                    if (j < n && ideograph(cells.get(j).codePoint())) {
                        while (i <= j) {
                            token.add(cells.get(i++));
                        }
                    }
                }
            } else if (i < n && cells.get(i).codePoint() != 32 && cells.get(i).codePoint() != 10) {
                token.add(cells.get(i++));
            }
            while (i < n && CLOSE.indexOf(cells.get(i).codePoint()) >= 0) {
                token.add(cells.get(i++));
            }
            if (token.isEmpty()) {
                token.add(cells.get(i++));
            }
            tokens.add(token);
        }
        return tokens;
    }

    static boolean word(int codePoint) {
        return codePoint >= 48 && codePoint <= 57 || codePoint >= 65 && codePoint <= 90 || codePoint >= 97 && codePoint <= 122 || WORD_SYMBOLS.indexOf(codePoint) >= 0;
    }

    static boolean digit(int codePoint) {
        return codePoint >= 48 && codePoint <= 57;
    }

    static boolean ideograph(int codePoint) {
        return codePoint >= 13312 && codePoint <= 40959 || codePoint >= 63744 && codePoint <= 64255;
    }

    static boolean fullWidthPunctuation(int codePoint) {
        return codePoint >= 12288 && codePoint <= 12351 || codePoint >= 65281 && codePoint <= 65295 || codePoint >= 65306 && codePoint <= 65312 || codePoint >= 65339 && codePoint <= 65344 || codePoint >= 65371 && codePoint <= 65381;
    }

    private static List<Cell> trim(List<Cell> line) {
        int start = 0;
        int end = line.size();
        while (start < end && line.get(start).codePoint() == 32) {
            start++;
        }
        while (end > start && line.get(end - 1).codePoint() == 32) {
            end--;
        }
        return new ArrayList<>(line.subList(start, end));
    }

    /** Two-line balanced wrap for centred quotes. */
    public static List<List<Cell>> balanced(List<Cell> cells, int measure, TextRenderer font) {
        List<List<Cell>> greedy = wrap(cells, measure, font, false);
        if (greedy.size() != 2) {
            return greedy;
        }
        String breaks = "，。；：！？、…」";
        int best = -1;
        int bestMax = Integer.MAX_VALUE;
        for (int i = 1; i < cells.size() - 1; i++) {
            if (breaks.indexOf(cells.get(i - 1).codePoint()) < 0 || breaks.indexOf(cells.get(i).codePoint()) >= 0) {
                continue;
            }
            int head = width(cells.subList(0, i), font);
            int tail = width(cells.subList(i, cells.size()), font);
            if (head > measure || tail > measure) {
                continue;
            }
            int longer = Math.max(head, tail);
            if (longer >= bestMax) {
                continue;
            }
            bestMax = longer;
            best = i;
        }
        int total = width(cells, font);
        if (best > 0 && bestMax <= total * 2 / 3) {
            return List.of(
                    new ArrayList<>(cells.subList(0, best)),
                    new ArrayList<>(cells.subList(best, cells.size())));
        }
        for (int tryWidth = total / 2; tryWidth <= measure; tryWidth++) {
            List<List<Cell>> wrapped = wrap(cells, tryWidth, font, false);
            if (wrapped.size() != 2) {
                continue;
            }
            return wrapped;
        }
        return greedy;
    }
}
