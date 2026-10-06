package io.github.arkosammy12.core.elements;

public final class SkipIfRegistersEqual extends SkipInstructionElement {

    private final int y;

    public SkipIfRegistersEqual(int offset, int x, int y) {
        super(offset, x);
        this.y = y;
    }

    @Override
    public SkipInstructionElement invertCondition() {
        return new SkipIfRegistersNotEqual(this.offset, this.x, this.y);
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromNibbles(0x5, this.x, this.y, 0x0));
    }

    @Override
    public String toString() {
        return "SkipIfRegistersEqual[%s, x=0x%01X, y=0x%01X]".formatted(this.getStringBaseContents(), this.x, this.y);
    }

}
