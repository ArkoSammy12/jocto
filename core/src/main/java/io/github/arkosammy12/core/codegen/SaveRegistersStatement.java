package io.github.arkosammy12.core.codegen;

public final class SaveRegistersStatement extends Statement {

    private final int begin;
    private final int end;

    public SaveRegistersStatement(int offset, int end) {
        super(offset);
        this.begin = 0;
        this.end = end;
    }

    public SaveRegistersStatement(int offset, int begin, int end) {
        super(offset);
        this.begin = begin;
        this.end = end;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
