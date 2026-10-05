package io.github.arkosammy12.core.elements;

public final class ScrollDownStatement extends Statement {

    private final int n;

    public ScrollDownStatement(int offset, int n) {
        super(offset);
        this.n = n;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromN(0x00C, this.n));
    }

}
