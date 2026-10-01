package io.github.arkosammy12.core.codegen;

public final class SetRegisterToKeyAssignment extends Assignment {

    public SetRegisterToKeyAssignment(int offset) {
        super(offset);
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
