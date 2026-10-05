package io.github.arkosammy12.core.elements;

public final class SetPitchAssignment extends Assignment {

    private final int x;

    public SetPitchAssignment(int offset, int x) {
        super(offset);
        this.x = x;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromXNN(0xF, this.x, 0x3A));
    }

    @Override
    public String toString() {
        return "SetPitchAssignment[%s, x=0x%01X]".formatted(this.getStringBaseContents(), this.x);
    }

}
