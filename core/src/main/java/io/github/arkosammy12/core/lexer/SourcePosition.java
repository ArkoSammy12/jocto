package io.github.arkosammy12.core.lexer;

public record SourcePosition(int row, int column) {

    public SourcePosition {
        if (this.row() < 0) {
            throw new IllegalArgumentException("Row cannot be less than zero!");
        }
        if (this.column() < 0) {
            throw new IllegalArgumentException("Column cannot be less than zero!");
        }
    }

    public SourcePosition fromRow(int row) {
        return new SourcePosition(row, this.column());
    }

    public SourcePosition fromColumn(int column) {
        return new SourcePosition(this.row(), column);
    }

}
