package io.github.arkosammy12.core.elements;

public abstract sealed class CodeElement permits InstructionElement, ByteElement, PointerElement {

    protected final int offset;

    public CodeElement(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return this.offset;
    }

    public abstract int getSizeInBytes();

    public abstract BytesResult getBytes();

    protected String getStringBaseContents() {
        return "offset=0x%03X, sizeInBytes=%d".formatted(this.offset, this.getSizeInBytes());
    }

    @Override
    public String toString() {
        return "CodePrimitive[%s]".formatted(this.getStringBaseContents());
    }

    public static byte[] fromOpcode(int opcode) {
        return fromBytePair((opcode >>> 8) & 0xFF, opcode & 0x00FF);
    }

    public static byte[] fromN(int upperThreeNibbles, int N) {
        return fromBytePair((upperThreeNibbles >>> 4) & 0xFF, ((upperThreeNibbles & 0xF) << 4) | (N & 0xF));
    }

    public static byte[] fromXNN(int firstNibble, int x, int NN) {
        return fromBytePair(((firstNibble & 0xF) << 4) | (x & 0xF), NN & 0xFF);
    }

    public static byte[] fromNNN(int firstNibble, int NNN) {
        return fromBytePair(((firstNibble & 0xF) << 4) | ((NNN >>> 8) & 0xF), NNN & 0xFF);
    }

    public static byte[] fromNibbles(int first, int second, int third, int fourth) {
        return fromBytePair(((first & 0xF) << 4) | (second & 0xF), ((third & 0xF) << 4) | (fourth & 0xF));
    }

    public static byte[] fromBytePair(int first, int second) {
        return new byte[] {(byte) (first & 0xFF), (byte) (second & 0xFF)};
    }

}
