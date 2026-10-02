package io.github.arkosammy12.core.codegen;

public final class PlaneStatement extends Statement {

    private final int n;

    public PlaneStatement(int offset, int n) {
        super(offset);
        this.n = n;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
