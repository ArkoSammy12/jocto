package io.github.arkosammy12.core.codegen;

public final class RightSubtractRegisterFromRegisterAssignment extends Assignment {

    private final int x;
    private final int y;

    public RightSubtractRegisterFromRegisterAssignment(int offset, int x, int y) {
        super(offset);
        this.x = x;
        this.y = y;
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
