package io.github.arkosammy12.core.codegen;

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

}
