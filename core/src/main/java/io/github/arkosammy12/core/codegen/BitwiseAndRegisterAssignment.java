package io.github.arkosammy12.core.codegen;

public final class BitwiseAndRegisterAssignment extends Assignment {

    private final int x;
    private final int y;

    public BitwiseAndRegisterAssignment(int offset, int x, int y) {
        super(offset);
        this.x = x;
        this.y = y;
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
