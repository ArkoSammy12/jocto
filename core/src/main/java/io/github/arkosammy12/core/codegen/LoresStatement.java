package io.github.arkosammy12.core.codegen;

public final class LoresStatement extends Statement {

    public LoresStatement(int offset) {
        super(offset);
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
