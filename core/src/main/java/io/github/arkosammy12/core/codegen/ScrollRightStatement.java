package io.github.arkosammy12.core.codegen;

public final class ScrollRightStatement extends Statement {

    public ScrollRightStatement(int offset) {
        super(offset);
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
