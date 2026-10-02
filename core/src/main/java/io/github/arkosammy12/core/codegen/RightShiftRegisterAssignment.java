package io.github.arkosammy12.core.codegen;

public final class RightShiftRegisterAssignment extends Assignment {

    private final int x;
    private final int y;

    public RightShiftRegisterAssignment(int offset, int x, int y) {
        super(offset);
        this.x = x;
        this.y = y;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
