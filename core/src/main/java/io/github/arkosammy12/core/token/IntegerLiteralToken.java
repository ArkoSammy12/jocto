package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public final class IntegerLiteralToken extends LiteralToken {

    private final int value;

    public IntegerLiteralToken(int value, String lexeme, SourcePosition sourcePosition) {
        super(lexeme, sourcePosition);
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public boolean isUnsigned4Bits() {
        return this.value >= 0 && this.value <= 15;
    }

    public boolean is8Bits() {
        return this.value >= -128 && this.value <= 255;
    }

    public boolean isUnsigned12Bits() {
        return isUnsigned12Bits(this.value);
    }

    public boolean isUnsigned16Bits() {
        return isUnsigned16Bits(this.value);
    }

    @Override
    public String toString() {
        return "IntegerLiteralToken[%s, address=%d]".formatted(this.getBaseStringContents(), this.value);
    }

    public static boolean isUnsigned12Bits(int value) {
        return value >= 0 && value <= 0xFFF;
    }

    public static boolean isUnsigned16Bits(int value) {
        return value >= 0 && value <= 0xFFFF;
    }

    public static Optional<IntegerLiteralToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        try {
            String check = lexeme;
            boolean negate = false;
            if (check.startsWith("-")) {
                negate = true;
                check = check.substring(1);
            } else if (check.startsWith("+")) {
                check = check.substring(1);
            }
            int value;
            if (check.startsWith("0x")) {
                value = Integer.parseInt(check.substring(2), 16);
            } else if (check.startsWith("0b")) {
                value = Integer.parseInt(check.substring(2), 2);
            } else {
                value = Integer.parseInt(check);
            }
            if (negate) {
                value = -value;
            }
            return Optional.of(new IntegerLiteralToken(value, lexeme, sourcePosition));
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }

}
