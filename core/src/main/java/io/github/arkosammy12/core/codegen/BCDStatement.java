package io.github.arkosammy12.core.codegen;

public final class BCDStatement extends Statement {

    private final int registerIndex;

    public BCDStatement(int offset, int registerIndex) {
        super(offset);
        this.registerIndex = registerIndex;
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }
}
