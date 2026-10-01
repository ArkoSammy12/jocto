package io.github.arkosammy12.core.codegen;

public final class SaveFlagsStatement extends Statement {

    private final int begin;
    private final int end;

    public SaveFlagsStatement(int offset, int end) {
        super(offset);
        this.begin = 0;
        this.end = end;
    }

    public SaveFlagsStatement(int offset, int begin, int end) {
        super(offset);
        this.begin = begin;
        this.end = end;
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
