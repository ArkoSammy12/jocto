package io.github.arkosammy12.core.codegen;

public final class LoadFlagsStatement extends Statement {

    private final int x;

    public LoadFlagsStatement(int offset, int x) {
        super(offset);
        this.x = x;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
