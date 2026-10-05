package io.github.arkosammy12.core.elements;

public final class SkipIfRegistersEqual extends SkipInstruction {

    private final int y;

    public SkipIfRegistersEqual(int offset, int x, int y) {
        super(offset, x);
        this.y = y;
    }

    @Override
    public SkipInstruction invertCondition() {
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

}
