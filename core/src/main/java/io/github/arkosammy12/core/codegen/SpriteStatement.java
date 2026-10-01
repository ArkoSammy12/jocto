package io.github.arkosammy12.core.codegen;

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
    int getSizeInBytes() {
        return 2;
    }

}
