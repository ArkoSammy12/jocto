package io.github.arkosammy12.core.codegen;

public final class SetBuzzerTimerAssignment extends Assignment {

    private final int x;

    public SetBuzzerTimerAssignment(int offset, int x) {
        super(offset);
        this.x = x;
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
