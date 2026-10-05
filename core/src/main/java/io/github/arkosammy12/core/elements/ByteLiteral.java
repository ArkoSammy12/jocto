package io.github.arkosammy12.core.elements;

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

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(new byte[] {(byte) (this.value & 0xFF)});
    }

}
