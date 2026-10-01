package io.github.arkosammy12.core.codegen;

public final class SetDelayTimerAssignment extends Assignment {

    private final int x;

    public SetDelayTimerAssignment(int offset, int x) {
        super(offset);
        this.x = x;
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
