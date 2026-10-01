package io.github.arkosammy12.core.codegen;

public final class ScrollLeftStatement extends Statement {

    public ScrollLeftStatement(int offset) {
        super(offset);
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
