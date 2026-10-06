package io.github.arkosammy12.core.elements;

public final class SkipIfRegisterEqualsConstant extends SkipInstructionElement {

    private final int nn;

    public SkipIfRegisterEqualsConstant(int offset, int x, int nn) {
        super(offset, x);
        this.nn = nn & 0xFF;
    }

    @Override
    public SkipInstructionElement invertCondition() {
        return new SkipIfRegisterNotEqualsConstant(this.offset, this.x, this.nn);
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromXNN(0x3, this.x, this.nn));
    }

    @Override
    public String toString() {
        return "SkipIfRegisterEqualsConstant[%s, x=0x%01X, nn=0x%02X]".formatted(this.getStringBaseContents(), this.x, this.nn);
    }

}
