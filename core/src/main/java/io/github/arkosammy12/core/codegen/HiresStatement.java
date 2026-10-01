package io.github.arkosammy12.core.codegen;

public final class HiresStatement extends Statement {

    public HiresStatement(int offset) {
        super(offset);
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
