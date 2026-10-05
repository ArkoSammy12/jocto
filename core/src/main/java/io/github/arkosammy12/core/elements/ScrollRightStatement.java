package io.github.arkosammy12.core.elements;

public final class ScrollRightStatement extends Statement {

    public ScrollRightStatement(int offset) {
        super(offset);
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromOpcode(0x00FB));
    }

}
