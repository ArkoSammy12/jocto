package io.github.arkosammy12.core.elements;

public final class SkipIfRegisterNotEqualsConstant extends SkipInstruction {

    private final int nn;

    public SkipIfRegisterNotEqualsConstant(int offset, int x, int nn) {
        super(offset, x);
        this.nn = nn & 0xFF;
    }

    @Override
    public SkipInstruction invertCondition() {
        return new SkipIfRegisterEqualsConstant(this.offset, this.x, this.nn);
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromXNN(0x4, this.x, this.nn));
    }

}
