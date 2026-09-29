package io.github.arkosammy12.core.lexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public SourceStream<SourceCharacter> createSourceCharacterStream() {
        return SourceStream.ofLines(this.lines);
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

    public Optional<SourcePosition> indexOf(String str) {
        for (int row = 0; row < this.getLineSize(); row++) {
            int column = this.getLine(row).indexOf(str);
            if (column >= 0) {
                return Optional.of(new SourcePosition(row, column));
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
        return this.subFile(new SourcePosition(beginRow, beginColumn), new SourcePosition(endRow, endColumn));
    }

    public SourceFile subFile(SourcePosition begin, SourcePosition end) {
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
        return this.remove(new SourcePosition(beginRow, beginColumn), new SourcePosition(endRow, endColumn));
    }

    public SourceFile remove(SourcePosition begin, SourcePosition end) {
        return this.subFile(new SourcePosition(0, 0), begin.fromRow(begin.row() + 1)).concatenate(this.subFile(end, new SourcePosition(this.getLineSize(), Integer.MAX_VALUE)));
    }

    @Override
    public String toString() {
        return String.join("\n", this.lines);
    }

}
