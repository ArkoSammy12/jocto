package io.github.arkosammy12.core.elements;

public final class SetRegisterToRandomAssignment extends Assignment {

    private final int x;
    private final int nn;

    public SetRegisterToRandomAssignment(int offset, int x, int nn) {
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
        return new BytesResult.Data(fromXNN(0xC, this.x, this.nn));
    }

}
