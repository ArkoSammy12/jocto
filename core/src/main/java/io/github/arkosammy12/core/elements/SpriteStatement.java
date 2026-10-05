package io.github.arkosammy12.core.elements;

public final class SpriteStatement extends Statement {

    private final int x;
    private final int y;
    private final int n;

    public SpriteStatement(int offset, int x, int y, int n) {
        super(offset);
        this.x = x;
        this.y = y;
        this.n = n;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return new BytesResult.Data(fromNibbles(0xD, this.x, this.y, this.n));
    }

    @Override
    public String toString() {
        return "SpriteStatement[%s, x=0x%01X, y=0x%01X, n=0x%01X]".formatted(this.getStringBaseContents(), this.x, this.y, this.n);
    }

}
