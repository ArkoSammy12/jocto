package io.github.arkosammy12.core.elements;

public final class LeftSubtractRegisterFromRegisterAssignment extends Assignment {

    private final int x;
    private final int y;

    public LeftSubtractRegisterFromRegisterAssignment(int offset, int x, int y) {
        super(offset);
        this.x = x;
        this.y = y;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromNibbles(0x8, this.x, this.y, 0x5));
    }

    @Override
    public String toString() {
        return "LeftSubtractRegisterFromRegisterAssignment[%s, x=0x%01X, y=0x%01X]".formatted(this.getStringBaseContents(), this.x, this.y);
    }

}
