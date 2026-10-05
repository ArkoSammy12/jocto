package io.github.arkosammy12.core.elements;

public final class ScrollUpStatement extends Statement {

    private final int n;

    public ScrollUpStatement(int offset, int n) {
        super(offset);
        this.n = n;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromN(0x00D, this.n));
    }

}
