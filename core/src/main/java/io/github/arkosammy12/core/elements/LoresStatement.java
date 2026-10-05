package io.github.arkosammy12.core.elements;

public final class LoresStatement extends Statement {

    public LoresStatement(int offset) {
        super(offset);
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromOpcode(0x00FE));
    }

    @Override
    public String toString() {
        return "LoresStatement[%s]".formatted(this.getStringBaseContents());
    }

}
