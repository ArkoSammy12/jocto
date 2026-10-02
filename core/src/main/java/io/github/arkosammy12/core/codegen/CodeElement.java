package io.github.arkosammy12.core.codegen;

public abstract sealed class CodeElement permits ByteLiteral, Instruction, InstructionBlock, PointerLiteral {

    private final int offset;

    public CodeElement(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return this.offset;
    }

    public abstract int getSizeInBytes();

    protected String getStringBaseContents() {
        return "offset=%04X, sizeInBytes=%d".formatted(this.offset, this.getSizeInBytes());
    }

    @Override
    public String toString() {
        return "CodeElement[%s]".formatted(this.getStringBaseContents());
    }

}
