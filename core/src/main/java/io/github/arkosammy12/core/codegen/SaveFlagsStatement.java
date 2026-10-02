package io.github.arkosammy12.core.codegen;

public final class SaveFlagsStatement extends Statement {

    private final int x;

    public SaveFlagsStatement(int offset, int x) {
        super(offset);
        this.x = x;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
