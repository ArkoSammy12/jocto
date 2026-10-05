package io.github.arkosammy12.core.elements;

public final class ExitStatement extends Statement {

    public ExitStatement(int offset) {
        super(offset);
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromOpcode(0x00FD));
    }

    @Override
    public String toString() {
        return "ExitStatement[%s]".formatted(this.getStringBaseContents());
    }

}
