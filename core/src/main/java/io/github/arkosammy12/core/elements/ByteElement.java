package io.github.arkosammy12.core.elements;

public final class ByteElement extends CodeElement {

    private final int value;

    public ByteElement(int offset, int value) {
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

    @Override
    public String toString() {
        return "BytePrimitive[%s, value=0x%02X]".formatted(this.getStringBaseContents(), this.value);
    }

}
