package io.github.arkosammy12.core.elements;

public final class ClearScreenStatement extends Statement {

    public ClearScreenStatement(int offset) {
        super(offset);
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromOpcode(0x00E0));
    }

    @Override
    public String toString() {
        return "ClearScreenStatement[%s]".formatted(this.getStringBaseContents());
    }

}
