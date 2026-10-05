package io.github.arkosammy12.core.elements;

public final class SetRegisterToConstantAssignment extends Assignment {

    private final int x;
    private final int nn;

    public SetRegisterToConstantAssignment(int offset, int x, int nn) {
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
        return new BytesResult.Data(fromXNN(0x6, this.x, this.nn));
    }

}
