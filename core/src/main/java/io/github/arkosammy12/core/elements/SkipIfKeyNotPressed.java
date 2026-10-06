package io.github.arkosammy12.core.elements;

public final class SkipIfKeyNotPressed extends SkipInstructionElement {

    public SkipIfKeyNotPressed(int offset, int x) {
        super(offset, x);
    }

    @Override
    public SkipInstructionElement invertCondition() {
        return new SkipIfKeyPressed(this.offset, this.x);
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromXNN(0xE, this.x, 0xA1));
    }

    @Override
    public String toString() {
        return "SkipIfKeyNotPressed[%s, x=0x%01X]".formatted(this.getStringBaseContents(), this.x);
    }

}
