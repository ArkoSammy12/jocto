package io.github.arkosammy12.core.codegen;

public final class SetRegisterToDelayTimerAssignment extends Assignment {

    private final int x;

    public SetRegisterToDelayTimerAssignment(int offset, int x) {
        super(offset);
        this.x = x;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
