package io.github.arkosammy12.core.codegen;

public abstract sealed class CodePrimitive extends CodeElement permits Instruction, ByteLiteral, PointerLiteral {

    protected final int offset;

    public CodePrimitive(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return this.offset;
    }

    public abstract int getSizeInBytes();

    protected String getStringBaseContents() {
        return "offset=%d, sizeInBytes=%d".formatted(this.offset, this.getSizeInBytes());
    }

    @Override
    public String toString() {
        return "CodePrimitive[%s]".formatted(this.getStringBaseContents());
    }

}
