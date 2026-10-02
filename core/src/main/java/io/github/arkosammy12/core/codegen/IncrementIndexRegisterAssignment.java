package io.github.arkosammy12.core.codegen;

public final class IncrementIndexRegisterAssignment extends Assignment {

    private final int x;

    public IncrementIndexRegisterAssignment(int offset, int x) {
        super(offset);
        this.x = x;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
