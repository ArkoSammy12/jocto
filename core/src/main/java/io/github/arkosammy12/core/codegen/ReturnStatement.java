package io.github.arkosammy12.core.codegen;

public final class ReturnStatement extends Statement {

    public ReturnStatement(int offset) {
        super(offset);
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
