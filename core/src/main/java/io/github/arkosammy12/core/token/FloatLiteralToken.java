package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public final class FloatLiteralToken extends NumberLiteralToken {

    private final double value;

    public FloatLiteralToken(double value, String lexeme, SourcePosition sourcePosition) {
        super(lexeme, sourcePosition);
        this.value = value;
    }

    public double getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "FloatLiteralToken[%s, address=%f]".formatted(this.getBaseStringContents(), this.value);
    }

    public static Optional<FloatLiteralToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        try {
            return Optional.of(new FloatLiteralToken(Double.parseDouble(lexeme), lexeme, sourcePosition));
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }

    public static int toUInt8JS(double x) {
        if (!Double.isFinite(x) || x == 0x0) {
            return 0;
        }
        double narrowed = truncate(x) % 256.0;
        if (narrowed < 0.0) {
            narrowed += 256.0;
        }
        return (int) narrowed;
    }

    public static int toInt32JS(double x) {
        if (!Double.isFinite(x) || x == 0.0) {
            return 0;
        }
        final double MODULUS = 4294967296.0;  // 2^32
        final double SIGN_BIT = 2147483648.0; // 2^31

        double narrowed = truncate(x) % MODULUS;
        if (narrowed < 0.0) {
            narrowed += MODULUS;
        }
        if (narrowed >= SIGN_BIT) {
            narrowed -= MODULUS;
        }
        return (int) narrowed;
    }

    public static double truncate(double x) {
        return (x < 0) ? Math.ceil(x) : Math.floor(x);
    }

}