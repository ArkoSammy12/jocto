package io.github.arkosammy12.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SourceFile {

    private final List<String> lines;

    public SourceFile(Path path) throws IOException {
        this(Files.readAllLines(path));
    }

    public SourceFile(List<String> lines) {
        this.lines = List.copyOf(lines);
    }

    public int getLineSize() {
        return this.lines.size();
    }

    public String getLine(int row) {
        if (row < 0) {
            throw new IndexOutOfBoundsException("Row cannot be less than zero!");
        }
        if (row >= this.getLineSize()) {
            throw new IndexOutOfBoundsException("Row cannot be greater than or equal to amount of lines!");
        }
        return this.lines.get(row);
    }

    public SourceFile concatenate(SourceFile sourceFile) {
        if (this.lines.isEmpty()) {
            return sourceFile;
        } else if (sourceFile.lines.isEmpty()) {
            return this;
        }

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < this.getLineSize() - 1; i++) {
            lines.add(this.getLine(i));
        }
        lines.add(this.lines.getLast() + sourceFile.lines.getFirst());
        for (int i = 1; i < sourceFile.getLineSize(); i++) {
            lines.add(sourceFile.getLine(i));
        }
        return new SourceFile(lines);
    }

    public Optional<Position> indexOf(String str) {
        for (int row = 0; row < this.getLineSize(); row++) {
            int column = this.getLine(row).indexOf(str);
            if (column >= 0) {
                return Optional.of(new Position(row, column));
            }
        }
        return Optional.empty();
    }

    public SourceFile subFile(int beginRow, int beginColumn) {
        return this.subFile(beginRow, beginColumn, this.getLineSize());
    }

    public SourceFile subFile(int beginRow, int beginColumn, int endRow) {
        return this.subFile(beginRow, beginColumn, endRow, Integer.MAX_VALUE);
    }

    public SourceFile subFile(int beginRow, int beginColumn, int endRow, int endColumn) {
        return this.subFile(new Position(beginRow, beginColumn), new Position(endRow, endColumn));
    }

    public SourceFile subFile(Position begin, Position end) {
        if (begin.row() < 0) {
            throw new IndexOutOfBoundsException("The beginRow index cannot be less than 0!");
        }
        if (begin.column() < 0) {
            throw new IndexOutOfBoundsException("The beginColumn index cannot be less than 0!");
        }
        if (end.row() < begin.row()) {
            throw new IndexOutOfBoundsException("The endRow index of '%d' cannot be less than the beginRow index of '%d'!".formatted(end.row(), begin.row()));
        }
        if (end.row() == begin.row() + 1 && end.column() < begin.column()) {
            throw new IndexOutOfBoundsException("The endColumn index of '%d' cannot be less than the beginColumn index of '%d' for a subfile within the same row!".formatted(end.column(), begin.column()));
        }
        if (end.row() > this.getLineSize()) {
            throw new IndexOutOfBoundsException("The endRow index of '%d' cannot be greater than the amount of lines in the source file of '%d'!".formatted(end.row(), this.getLineSize()));
        }

        List<String> result = new ArrayList<>();
        for (int row = begin.row(); row < end.row(); row++) {
            String line = this.lines.get(row);
            int endColumn = end.column();
            if (row == begin.row()) {
                int originalLength = line.length();
                line = line.substring(Math.min(line.length(), begin.column()));

                // Adjust the end column offset as the one provided was relative to the original length of the row
                endColumn -= originalLength - line.length();
            }
            if (row == end.row() - 1) {
                line = line.substring(0, Math.min(line.length(), endColumn));
            }
            result.add(line);
        }
        return new SourceFile(result);
    }

    public SourceFile remove(int beginRow, int beginColumn) {
        return this.remove(beginRow, beginColumn, this.getLineSize());
    }

    public SourceFile remove(int beginRow, int beginColumn, int endRow) {
        return this.remove(beginRow, beginColumn, endRow, Integer.MAX_VALUE);
    }

    public SourceFile remove(int beginRow, int beginColumn, int endRow, int endColumn) {
        return this.remove(new Position(beginRow, beginColumn), new Position(endRow, endColumn));
    }

    public SourceFile remove(Position begin, Position end) {
        return this.subFile(new Position(0, 0), begin.fromRow(begin.row() + 1)).concatenate(this.subFile(end, new Position(this.getLineSize(), Integer.MAX_VALUE)));
    }

    @Override
    public String toString() {
        return String.join("\n", this.lines);
    }

}
