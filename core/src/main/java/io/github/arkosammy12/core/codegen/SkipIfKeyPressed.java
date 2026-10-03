package io.github.arkosammy12.core.codegen;

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

}
