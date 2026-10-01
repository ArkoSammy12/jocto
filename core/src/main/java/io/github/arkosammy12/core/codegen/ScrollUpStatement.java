package io.github.arkosammy12.core.codegen;

public final class ScrollUpStatement extends Statement {

    private final int n;

    public ScrollUpStatement(int offset, int n) {
        super(offset);
        this.n = n;
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
