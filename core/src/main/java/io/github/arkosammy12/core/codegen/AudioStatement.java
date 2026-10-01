package io.github.arkosammy12.core.codegen;

public final class AudioStatement extends Statement {

    public AudioStatement(int offset) {
        super(offset);
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
