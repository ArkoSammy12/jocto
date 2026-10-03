package io.github.arkosammy12.core.codegen;

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
}
