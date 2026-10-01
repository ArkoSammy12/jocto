package io.github.arkosammy12.core.codegen;

public abstract sealed class CodeElement permits Instruction, InstructionBlock {

    private final int offset;

    public CodeElement(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return this.offset;
    }

    abstract int getSizeInBytes();

}
