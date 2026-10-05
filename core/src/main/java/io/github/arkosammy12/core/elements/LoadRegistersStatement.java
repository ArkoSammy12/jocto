package io.github.arkosammy12.core.elements;

public final class LoadRegistersStatement extends Statement {

    private final int begin;
    private final int end;
    private final boolean customRange;

    public LoadRegistersStatement(int offset, int end) {
        super(offset);
        this.begin = 0;
        this.end = end;
        this.customRange = false;
    }

    public LoadRegistersStatement(int offset, int begin, int end) {
        super(offset);
        this.begin = begin;
        this.end = end;
        this.customRange = true;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(this.customRange ? fromNibbles(0x5, this.begin, this.end, 0x3) : fromXNN(0xF, this.end, 0x65));
    }

    @Override
    public String toString() {
        return "LoadRegistersStatement[%s, customRange=%s, begin=0x%01X, end=0x%01X]".formatted(this.getStringBaseContents(), this.customRange, this.begin, this.end);
    }

}
