package io.github.arkosammy12.core.codegen;

public final class ScrollDownStatement extends Statement {

    private final int n;

    public ScrollDownStatement(int offset, int n) {
        super(offset);
        this.n = n;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
