package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public final class FloatLiteralToken extends LiteralToken {

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
        return "FloatLiteralToken[%s, value=%f]".formatted(this.getBaseStringContents(), this.value);
    }

    public static Optional<FloatLiteralToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        try {
            return Optional.of(new FloatLiteralToken(Double.parseDouble(lexeme), lexeme, sourcePosition));
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }

}
