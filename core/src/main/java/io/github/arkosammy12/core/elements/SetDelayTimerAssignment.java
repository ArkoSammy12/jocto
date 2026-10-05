package io.github.arkosammy12.core.elements;

public final class SetDelayTimerAssignment extends Assignment {

    private final int x;

    public SetDelayTimerAssignment(int offset, int x) {
        super(offset);
        this.x = x;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromXNN(0xF, this.x, 0x15));
    }

    @Override
    public String toString() {
        return "SetDelayTimerAssignment[%s, x=0x%01X]".formatted(this.getStringBaseContents(), this.x);
    }

}
