package io.github.arkosammy12.core.codegen;

public final class SetRegisterToRandomAssignment extends Assignment {

    private final int nn;

    public SetRegisterToRandomAssignment(int offset, int nn) {
        super(offset);
        this.nn = nn & 0xFF;
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
