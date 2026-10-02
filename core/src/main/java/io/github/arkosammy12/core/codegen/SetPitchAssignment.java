package io.github.arkosammy12.core.codegen;

public final class SetPitchAssignment extends Assignment {

    private final int x;

    public SetPitchAssignment(int offset, int x) {
        super(offset);
        this.x = x;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
