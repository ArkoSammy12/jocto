package io.github.arkosammy12.core.codegen;

public final class SetRegisterToKeyAssignment extends Assignment {

    private final int x;

    public SetRegisterToKeyAssignment(int offset, int x) {
        super(offset);
        this.x = x;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
