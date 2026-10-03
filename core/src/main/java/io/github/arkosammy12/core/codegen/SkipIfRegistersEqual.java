package io.github.arkosammy12.core.codegen;

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

}
