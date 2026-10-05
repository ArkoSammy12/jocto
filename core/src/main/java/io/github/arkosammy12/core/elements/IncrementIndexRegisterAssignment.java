package io.github.arkosammy12.core.elements;

public final class IncrementIndexRegisterAssignment extends Assignment {

    private final int x;

    public IncrementIndexRegisterAssignment(int offset, int x) {
        super(offset);
        this.x = x;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromXNN(0xF, this.x, 0x1E));
    }

}
