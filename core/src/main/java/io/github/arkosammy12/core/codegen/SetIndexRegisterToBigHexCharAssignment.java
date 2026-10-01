package io.github.arkosammy12.core.codegen;

public final class SetIndexRegisterToBigHexCharAssignment extends Assignment {

    private final int x;

    public SetIndexRegisterToBigHexCharAssignment(int offset, int x) {
        super(offset);
        this.x = x;
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
