package org.agmas.noellesroles.client.screen.book;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Typesets reader content (sections, paragraphs, bullets, dividers) into
 * positioned ink lines plus brass/ink ornaments on a parchment page.
 */
public final class BookPage {
    public static final int LINE_HEIGHT = 12;

    private static final int SECTION_GAP = 10;
    private static final int SECTION_AFTER = 3;
    private static final int SECTION_LABEL_X = 9;
    private static final int SECTION_RULE_GAP = 5;
    private static final int SECTION_RULE_MIN = 16;
    private static final int BULLET_GAP = 4;
    private static final int BULLET_INDENT = 10;
    private static final int PARAGRAPH_GAP = 6;
    private static final int TAIL_GAP = 14;
    private static final int TAIL_AFTER = 6;

    private BookPage() {
    }

    public static Builder builder(TextRenderer font, int width) {
        return new Builder(font, width);
    }

    public enum OrnamentKind {
        SECTION_MARK,
        SECTION_RULE,
        BULLET,
        TAILPIECE
    }

    public record Ornament(OrnamentKind kind, int x, int y, int width, int height, int color, Text label) {
    }

    public record RenderedLine(OrderedText text, int indent, int y) {
    }

    public record Layout(List<RenderedLine> lines, List<Ornament> ornaments, int height) {
        public static final Layout EMPTY = new Layout(List.of(), List.of(), 0);

        /** Label of the section covering the given body-relative y, if any. */
        public Text sectionAt(int bodyY) {
            Text current = null;
            for (Ornament ornament : this.ornaments) {
                if (ornament.kind() != OrnamentKind.SECTION_MARK || ornament.y() - 4 > bodyY) {
                    continue;
                }
                current = ornament.label();
            }
            return current;
        }
    }

    public static final class Builder {
        private final TextRenderer font;
        private final int width;
        private final List<RenderedLine> lines = new ArrayList<>();
        private final List<Ornament> ornaments = new ArrayList<>();
        private int y;
        private boolean first = true;

        private Builder(TextRenderer font, int width) {
            this.font = font;
            this.width = Math.max(1, width);
        }

        public Builder section(Text label, int tone) {
            if (!this.first) {
                this.y += SECTION_GAP;
            }
            List<BookText.Cell> cells = BookText.normalise(BookText.cells(label, tone));
            List<List<BookText.Cell>> wrapped = BookText.wrap(cells, this.width - SECTION_LABEL_X, this.font, false);
            this.ornaments.add(new Ornament(OrnamentKind.SECTION_MARK, 0, this.y + 4, 5, 5, tone, Text.literal(BookText.string(cells))));
            for (int i = 0; i < wrapped.size(); i++) {
                this.lines.add(new RenderedLine(BookText.toOrderedText(wrapped.get(i)), SECTION_LABEL_X, this.y + 2 + i * LINE_HEIGHT));
            }
            if (wrapped.size() == 1) {
                int ruleX = SECTION_LABEL_X + BookText.width(wrapped.get(0), this.font) + SECTION_RULE_GAP;
                if (this.width - ruleX >= SECTION_RULE_MIN) {
                    this.ornaments.add(new Ornament(OrnamentKind.SECTION_RULE, ruleX, this.y + 6, this.width - ruleX, 1, BookPalette.INK_RULE, null));
                }
            }
            this.y += wrapped.size() * LINE_HEIGHT + SECTION_AFTER;
            this.first = false;
            return this;
        }

        public Builder paragraph(Text text, int indent) {
            if (!this.first) {
                this.y += PARAGRAPH_GAP;
            }
            List<BookText.Cell> cells = BookText.normalise(BookText.emphasised(text, BookPalette.INK, BookPalette.P_ITEM, BookPalette.P_MONEY));
            for (List<BookText.Cell> line : BookText.wrap(cells, this.width - indent, this.font, true)) {
                this.lines.add(new RenderedLine(BookText.toOrderedText(line), indent, this.y + 2));
                this.y += LINE_HEIGHT;
            }
            this.first = false;
            return this;
        }

        public Builder bullet(List<BookText.Cell> cellsIn) {
            if (!this.first) {
                this.y += BULLET_GAP;
            }
            this.ornaments.add(new Ornament(OrnamentKind.BULLET, 3, this.y + 5, 3, 3, BookPalette.BRASS_LO, null));
            List<BookText.Cell> cells = BookText.normalise(new ArrayList<>(cellsIn));
            for (List<BookText.Cell> line : BookText.wrap(cells, this.width - BULLET_INDENT, this.font, true)) {
                this.lines.add(new RenderedLine(BookText.toOrderedText(line), BULLET_INDENT, this.y + 2));
                this.y += LINE_HEIGHT;
            }
            this.first = false;
            return this;
        }

        public Layout finish() {
            this.y += TAIL_GAP;
            this.ornaments.add(new Ornament(OrnamentKind.TAILPIECE, 0, this.y, this.width, 5, BookPalette.INK_FAINT, null));
            this.y += TAIL_AFTER + 5;
            return new Layout(this.lines, this.ornaments, this.y);
        }
    }
}
