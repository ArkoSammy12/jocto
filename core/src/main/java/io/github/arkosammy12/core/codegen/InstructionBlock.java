package io.github.arkosammy12.core.codegen;

public sealed abstract class InstructionBlock extends CodeElement permits IfBlock, LoopBlock {

    public InstructionBlock(int offset) {
        super(offset);
    }

}
