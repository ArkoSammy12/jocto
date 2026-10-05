package io.github.arkosammy12.core.elements;

public final class SkipIfKeyNotPressed extends SkipInstruction {

    public SkipIfKeyNotPressed(int offset, int x) {
        super(offset, x);
    }

    @Override
    public SkipInstruction invertCondition() {
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

}
