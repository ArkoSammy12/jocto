package io.github.arkosammy12.core.codegen;

public final class ClearStatement extends Statement {

    public ClearStatement(int offset) {
        super(offset);
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
