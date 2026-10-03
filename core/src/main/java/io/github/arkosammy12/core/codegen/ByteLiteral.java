package io.github.arkosammy12.core.codegen;

public final class ByteLiteral extends CodePrimitive {

    private final int value;

    public ByteLiteral(int offset, int value) {
        super(offset);
        this.value = value & 0xFF;
    }

    @Override
    public int getSizeInBytes() {
        return 1;
    }

}
