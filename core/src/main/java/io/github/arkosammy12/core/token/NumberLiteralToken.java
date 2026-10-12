package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;

public sealed abstract class NumberLiteralToken extends LiteralToken permits IntegerLiteralToken, FloatLiteralToken {

    public NumberLiteralToken(String lexeme, SourcePosition sourcePosition) {
        super(lexeme, sourcePosition);
    }

    abstract public int getIntValue();

    abstract public double getDoubleValue();

    public boolean isUnsigned4Bits() {
        int value = this.getIntValue();
        return value >= 0 && value <= 15;
    }

    public boolean is8Bits() {
        int value = this.getIntValue();
        return value >= -128 && value <= 255;
    }

    public boolean isUnsigned12Bits() {
        return isUnsigned12Bits(this.getIntValue());
    }

    public boolean isUnsigned16Bits() {
        return isUnsigned16Bits(this.getIntValue());
    }

    public static boolean isUnsigned12Bits(int value) {
        return value >= 0 && value <= 0xFFF;
    }

    public static boolean isUnsigned16Bits(int value) {
        return value >= 0 && value <= 0xFFFF;
    }

}
