package io.github.arkosammy12.core.elements;

public final class AudioStatement extends Statement {

    public AudioStatement(int offset) {
        super(offset);
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromOpcode(0xF002));
    }

}
