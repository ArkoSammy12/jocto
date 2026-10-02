package io.github.arkosammy12.core.codegen;

public final class IfBlock extends InstructionBlock {

    public IfBlock(int offset) {
        super(offset);
    }

    @Override
    public int getSizeInBytes() {
        return 0;
    }

}
