package io.github.arkosammy12.core.codegen;

public final class SetRegisterToDelayTimerAssignment extends Assignment {

    public SetRegisterToDelayTimerAssignment(int offset) {
        super(offset);
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
