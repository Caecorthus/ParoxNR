package org.agmas.noellesroles.client.screen.book;

/** Small integer rectangle shared by the book-styled screens. */
public record BookRect(int x, int y, int width, int height) {
    public int right() {
        return this.x + this.width;
    }

    public int bottom() {
        return this.y + this.height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.right() && mouseY >= this.y && mouseY < this.bottom();
    }

    public BookRect shifted(int dx, int dy) {
        return new BookRect(this.x + dx, this.y + dy, this.width, this.height);
    }
}
