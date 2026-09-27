package io.github.arkosammy12.core.util;

public record Position(int row, int column) {

    public Position {
        if (this.row() < 0) {
            throw new IllegalArgumentException("Row cannot be less than zero!");
        }
        if (this.column() < 0) {
            throw new IllegalArgumentException("Column cannot be less than zero!");
        }
    }

    public Position fromRow(int row) {
        return new Position(row, this.column());
    }

    public Position fromColumn(int column) {
        return new Position(this.row(), column);
    }

}
