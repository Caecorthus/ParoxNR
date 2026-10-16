package org.agmas.noellesroles.client.screen.book;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Drawing primitives for the book-styled role info screen:
 * panelled chrome, parchment sheets, gilt ornaments and bitmap icons.
 */
public final class BookPaint {
    private static final String ELLIPSIS = "…";

    private static final String[] STAR = {"...#...", "..###..", "#######", ".#####.", "..###..", ".##.##.", ".#...#."};
    private static final String[] DIAMOND = {"..#..", ".###.", "#####", ".###.", "..#.."};
    private static final String[] MAGNIFIER = {".####....", "#....#...", "#....#...", "#....#...", "#....#...", ".######..", ".....###.", "......###", ".......##"};
    private static final String[] CLOSE = {"#.....#", "##...##", ".##.##.", "..###..", ".##.##.", "##...##", "#.....#"};
    private static final String[] CLEAR = {"#...#", ".#.#.", "..#..", ".#.#.", "#...#"};
    private static final String[] FOLD_UP = {"..#..", ".#.#.", "#...#"};
    private static final String[] FOLD_DOWN = {"#...#", ".#.#.", "..#.."};
    private static final String[] BULLET = {".#.", "###", ".#."};
    private static final String[] BOOK = {"###########", "#....#....#", "#....#....#", "#....#....#", "#....#....#", "#....#....#", "#....#....#", "###########"};

    private BookPaint() {
    }

    public static void roundedFill(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x + 1, y, x + w - 1, y + 1, col);
        c.fill(x, y + 1, x + w, y + h - 1, col);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, col);
    }

    public static void roundedOutline(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x + 1, y, x + w - 1, y + 1, col);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, col);
        c.fill(x, y + 1, x + 1, y + h - 1, col);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
    }

    public static void dropShadow(DrawContext c, int x, int y, int w, int h) {
        c.fill(x + w, y + 2, x + w + 1, y + h, BookPalette.SHADOW);
        c.fill(x + 2, y + h, x + w, y + h + 1, BookPalette.SHADOW);
    }

    public static void bevel(DrawContext c, int x, int y, int w, int h) {
        c.fill(x + 1, y + 1, x + w - 1, y + 2, BookPalette.RIM_HI);
        c.fill(x + 1, y + 2, x + 2, y + h - 1, BookPalette.RIM_HI);
        c.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, BookPalette.RIM_LO);
        c.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, BookPalette.RIM_LO);
        c.fill(x + w - 2, y + 1, x + w - 1, y + 2, BookPalette.RIM_MITER);
        c.fill(x + 1, y + h - 2, x + 2, y + h - 1, BookPalette.RIM_MITER);
    }

    public static void ring(DrawContext c, int x, int y, int w, int h, int top, int bottom) {
        c.fill(x + 2, y + 2, x + w - 2, y + 3, top);
        c.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, bottom);
        c.fillGradient(x + 2, y + 3, x + 3, y + h - 3, top, bottom);
        c.fillGradient(x + w - 3, y + 3, x + w - 2, y + h - 3, top, bottom);
    }

    /** Brass-framed dark panel used for the directory and the reader frame. */
    public static void panel(DrawContext c, int x, int y, int w, int h) {
        dropShadow(c, x, y, w, h);
        roundedOutline(c, x, y, w, h, BookPalette.EDGE);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, BookPalette.BODY);
        bevel(c, x, y, w, h);
        ring(c, x, y, w, h, BookPalette.BRASS_HI, BookPalette.BRASS_LO);
    }

    public static void titleBand(DrawContext c, int x, int y, int w) {
        c.fill(x + 3, y + 3, x + w - 3, y + 19, BookPalette.BAND);
        c.fill(x + 3, y + 19, x + w - 3, y + 20, BookPalette.BRASS_LO);
        c.fill(x + 3, y + 20, x + w - 3, y + 21, BookPalette.EDGE);
    }

    public static void etched(DrawContext c, int x1, int x2, int y) {
        c.fill(x1, y, x2, y + 1, BookPalette.ETCH_DARK);
        c.fill(x1, y + 1, x2, y + 2, BookPalette.ETCH_LIGHT);
    }

    public static void iconWash(DrawContext c, int bx, int by, int col) {
        roundedFill(c, bx, by, 14, 14, col);
    }

    public static void rowState(DrawContext c, int x1, int y, int x2, int h, boolean hovered, boolean selected) {
        if (selected) {
            c.fill(x1, y, x2, y + h, BookPalette.SELECT);
            c.fill(x1, y, x1 + 2, y + h, BookPalette.COIN);
        } else if (hovered) {
            c.fill(x1, y, x2, y + h, BookPalette.HOVER);
        }
    }

    public static void scrollbarDark(DrawContext c, int gx, int vy, int vh, int ty, int th, boolean hot) {
        c.fill(gx + 1, vy, gx + 2, vy + vh, BookPalette.EDGE);
        int body = hot ? BookPalette.BRASS : BookPalette.BRASS_LO;
        int light = hot ? BookPalette.BRASS_HI : BookPalette.BRASS;
        int shade = hot ? BookPalette.BRASS_LO : BookPalette.THUMB_SHADE;
        c.fill(gx, ty, gx + 3, ty + th, body);
        c.fill(gx, ty, gx + 1, ty + th, light);
        c.fill(gx, ty, gx + 3, ty + 1, light);
        c.fill(gx + 2, ty + 1, gx + 3, ty + th, shade);
        c.fill(gx + 1, ty + th - 1, gx + 3, ty + th, shade);
    }

    public static void scrollbarPaper(DrawContext c, int gx, int vy, int vh, int ty, int th, boolean hot) {
        c.fill(gx + 1, vy, gx + 2, vy + vh, BookPalette.INK_RULE_SOFT);
        c.fill(gx, ty, gx + 3, ty + th, hot ? BookPalette.INK_MUTED : BookPalette.PAPER_THUMB);
        c.fill(gx, ty, gx + 3, ty + 1, hot ? BookPalette.PAPER_THUMB : BookPalette.PAPER_THUMB_HI);
    }

    public static int thumbSize(int track, int viewport, int total) {
        return Math.max(12, track * viewport / Math.max(1, total));
    }

    public static int thumbOffset(int trackTop, int track, int thumb, int scroll, int viewport, int total) {
        return trackTop + scroll * (track - thumb) / Math.max(1, total - viewport);
    }

    public static void fadeTop(DrawContext c, int x1, int y, int x2, int rgb) {
        c.fillGradient(x1, y, x2, y + 8, 1, 0xFF000000 | (rgb & 0xFFFFFF), rgb & 0xFFFFFF);
    }

    public static void fadeBottom(DrawContext c, int x1, int y2, int x2, int rgb) {
        c.fillGradient(x1, y2 - 8, x2, y2, 1, rgb & 0xFFFFFF, 0xFF000000 | (rgb & 0xFFFFFF));
    }

    /** Parchment sheet with edge shade, top glint and folded-corner details. */
    public static void paper(DrawContext c, int x, int y, int w, int h) {
        c.fill(x, y, x + w, y + h, BookPalette.PAPER_EDGE);
        c.fill(x + 2, y + 2, x + w - 2, y + h - 2, BookPalette.PAPER);
        c.fill(x + 1, y + h - 1, x + w, y + h, BookPalette.PAPER_DEEP);
        c.fill(x + w - 1, y + 1, x + w, y + h, BookPalette.PAPER_DEEP);
        c.fill(x + 1, y + 1, x + w - 1, y + 2, BookPalette.PAPER_GLINT);
        foxCorner(c, x, y, 1, 1);
        foxCorner(c, x + w - 1, y, -1, 1);
        foxCorner(c, x, y + h - 1, 1, -1);
        foxCorner(c, x + w - 1, y + h - 1, -1, -1);
    }

    private static void foxCorner(DrawContext c, int cx, int cy, int dx, int dy) {
        c.fill(cx, cy, cx + 1, cy + 1, BookPalette.PAPER_DEEP);
        c.fill(cx + dx, cy, cx + dx + 1, cy + 1, BookPalette.PAPER_DEEP);
        c.fill(cx, cy + dy, cx + 1, cy + dy + 1, BookPalette.PAPER_DEEP);
    }

    /** 5x5 faceted gem icon tinted with the given rgb. */
    public static void gem(DrawContext c, int x, int y, int rgb, boolean bright) {
        String[] shape = {".BBB.", "BGIIB", "BIIIB", "BIISB", ".BBB."};
        int identity = 0xFF000000 | (rgb & 0xFFFFFF);
        int bezel = bright ? BookPalette.BRASS_HI : BookPalette.BRASS_LO;
        int glint = mix(identity, 0xFFFFFFFF, 0.45);
        int shade = mix(identity, 0xFF000000, 0.35);
        for (int row = 0; row < shape.length; row++) {
            String line = shape[row];
            int start = 0;
            while (start < line.length()) {
                char key = line.charAt(start);
                int end = start + 1;
                while (end < line.length() && line.charAt(end) == key) {
                    end++;
                }
                int col = switch (key) {
                    case 'B' -> bezel;
                    case 'G' -> glint;
                    case 'S' -> shade;
                    case 'I' -> identity;
                    default -> 0;
                };
                if (col != 0) {
                    c.fill(x + start, y + row, x + end, y + row + 1, col);
                }
                start = end;
            }
        }
    }

    public static void gem2x(DrawContext c, int x, int y, int rgb) {
        c.getMatrices().push();
        c.getMatrices().translate(x, y, 0.0F);
        c.getMatrices().scale(2.0F, 2.0F, 1.0F);
        gem(c, 0, 0, rgb, false);
        c.getMatrices().pop();
    }

    public static void star(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, STAR);
    }

    public static void diamond(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, DIAMOND);
    }

    public static void magnifier(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, MAGNIFIER);
    }

    public static void close7(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, CLOSE);
    }

    public static void clear5(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, CLEAR);
    }

    public static void foldUp(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, FOLD_UP);
    }

    public static void foldDown(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, FOLD_DOWN);
    }

    public static void bullet(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, BULLET);
    }

    public static void bookIcon(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, BOOK);
    }

    public static void chevron(DrawContext c, int x, int y, boolean expanded, int col) {
        if (expanded) {
            c.fill(x, y + 1, x + 5, y + 2, col);
            c.fill(x + 1, y + 2, x + 4, y + 3, col);
            c.fill(x + 2, y + 3, x + 3, y + 4, col);
        } else {
            c.fill(x + 1, y, x + 2, y + 5, col);
            c.fill(x + 2, y + 1, x + 3, y + 4, col);
            c.fill(x + 3, y + 2, x + 4, y + 3, col);
        }
    }

    public static void chevronLeft(DrawContext c, int x, int y, int col) {
        c.fill(x + 3, y, x + 4, y + 5, col);
        c.fill(x + 2, y + 1, x + 3, y + 4, col);
        c.fill(x + 1, y + 2, x + 2, y + 3, col);
    }

    public static void bitmap(DrawContext c, int x, int y, int col, String... rows) {
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            int start = line.indexOf('#');
            while (start >= 0) {
                int end = start + 1;
                while (end < line.length() && line.charAt(end) == '#') {
                    end++;
                }
                c.fill(x + start, y + row, x + end, y + row + 1, col);
                start = line.indexOf('#', end);
            }
        }
    }

    public static void brassTooltip(DrawContext c, int x, int y, int w, int h) {
        int bg = BookPalette.TIP_BG;
        c.fill(x - 3, y - 4, x + w + 3, y - 3, bg);
        c.fill(x - 3, y + h + 3, x + w + 3, y + h + 4, bg);
        c.fill(x - 3, y - 3, x + w + 3, y + h + 3, bg);
        c.fill(x - 4, y - 3, x - 3, y + h + 3, bg);
        c.fill(x + w + 3, y - 3, x + w + 4, y + h + 3, bg);
        c.fillGradient(x - 3, y - 2, x - 2, y + h + 2, BookPalette.BRASS_HI, BookPalette.BRASS_LO);
        c.fillGradient(x + w + 2, y - 2, x + w + 3, y + h + 2, BookPalette.BRASS_HI, BookPalette.BRASS_LO);
        c.fill(x - 3, y - 3, x + w + 3, y - 2, BookPalette.BRASS_HI);
        c.fill(x - 3, y + h + 2, x + w + 3, y + h + 3, BookPalette.BRASS_LO);
    }

    public static int tooltipHeight(int lines) {
        return lines <= 1 ? 8 : 10 * lines;
    }

    public static int tooltipWidth(TextRenderer font, List<Text> lines) {
        int width = 0;
        for (Text line : lines) {
            width = Math.max(width, font.getWidth(line));
        }
        return width;
    }

    public static void tooltip(DrawContext c, TextRenderer font, List<Text> lines, int x, int y, int w) {
        int h = tooltipHeight(lines.size());
        c.getMatrices().push();
        c.getMatrices().translate(0.0F, 0.0F, 400.0F);
        brassTooltip(c, x, y, w, h);
        int lineY = y;
        for (int i = 0; i < lines.size(); i++) {
            c.drawText(font, lines.get(i), x, lineY, i == 0 ? BookPalette.TEXT_HI : 0xFFCDBB9C, true);
            lineY += i == 0 ? 12 : 10;
        }
        c.getMatrices().pop();
    }

    public static String ellipsize(TextRenderer font, String text, int maxWidth) {
        if (font.getWidth(text) <= maxWidth) {
            return text;
        }
        StringBuilder kept = new StringBuilder();
        for (int codePoint : text.codePoints().toArray()) {
            int before = kept.length();
            kept.appendCodePoint(codePoint);
            if (font.getWidth(kept + ELLIPSIS) <= maxWidth) {
                continue;
            }
            kept.setLength(before);
            break;
        }
        return kept.toString().stripTrailing() + ELLIPSIS;
    }

    public static int mix(int a, int b, double t) {
        int alpha = (int) Math.round((a >>> 24) * (1.0 - t) + (b >>> 24) * t);
        int red = (int) Math.round((a >> 16 & 0xFF) * (1.0 - t) + (b >> 16 & 0xFF) * t);
        int green = (int) Math.round((a >> 8 & 0xFF) * (1.0 - t) + (b >> 8 & 0xFF) * t);
        int blue = (int) Math.round((a & 0xFF) * (1.0 - t) + (b & 0xFF) * t);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }
}
