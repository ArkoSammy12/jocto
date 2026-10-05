package io.github.arkosammy12.core.elements;

public final class PlaneStatement extends Statement {

    private final int n;

    public PlaneStatement(int offset, int n) {
        super(offset);
        this.n = n;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromXNN(0xF, this.n, 0x01));
    }

}
