package io.github.arkosammy12.core.codegen;

public abstract sealed class CodePrimitive extends CodeElement permits Instruction, ByteLiteral, PointerLiteral {

    public CodePrimitive(int offset) {
        super(offset);
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
