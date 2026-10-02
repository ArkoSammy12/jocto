package io.github.arkosammy12.core.codegen;

public final class LoopBlock extends InstructionBlock {

    public LoopBlock(int offset) {
        super(offset);
    }

    @Override
    public int getSizeInBytes() {
        return 0;
    }

}
