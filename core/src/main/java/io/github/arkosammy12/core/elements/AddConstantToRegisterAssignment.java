package io.github.arkosammy12.core.elements;

public final class AddConstantToRegisterAssignment extends Assignment {

    private final int x;
    private final int nn;

    public AddConstantToRegisterAssignment(int offset, int x, int nn) {
        super(offset);
        this.x = x;
        this.nn = nn & 0xFF;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromXNN(0x7, this.x, this.nn));
    }

}
