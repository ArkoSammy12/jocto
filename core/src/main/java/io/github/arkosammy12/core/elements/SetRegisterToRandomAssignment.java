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

    @Override
    public String toString() {
        return "SetRegisterToRandomAssignment[%s, x=0x%01X, nn=0x%02X]".formatted(this.getStringBaseContents(), this.x, this.nn);
    }

}
