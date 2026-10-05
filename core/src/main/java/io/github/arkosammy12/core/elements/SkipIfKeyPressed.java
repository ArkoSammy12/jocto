package io.github.arkosammy12.core.elements;

public final class SkipIfKeyPressed extends SkipInstruction {

    public SkipIfKeyPressed(int offset, int x) {
        super(offset, x);
    }

    @Override
    public SkipInstruction invertCondition() {
        return new SkipIfKeyNotPressed(this.offset, this.x);
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromXNN(0xE, this.x, 0x9E));
    }

    @Override
    public String toString() {
        return "SkipIfKeyPressed[%s, x=0x%01X]".formatted(this.getStringBaseContents(), this.x);
    }

}
