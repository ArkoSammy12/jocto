package io.github.arkosammy12.jocto.core.token;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;

public abstract sealed class NumberLiteralToken extends LiteralToken permits FloatLiteralToken, IntegerLiteralToken {

    public NumberLiteralToken(String lexeme, SourcePosition sourcePosition) {
        super(lexeme, sourcePosition);
    }

    public static boolean isUnsigned4Bits(int value) {
        return value >= 0 && value <= 15;
    }

    public static boolean isUnsigned12Bits(int value) {
        return value >= 0 && value <= 0xFFF;
    }

    public static boolean isUnsigned16Bits(int value) {
        return value >= 0 && value <= 0xFFFF;
    }

    public static Optional<? extends LiteralToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        Optional<IntegerLiteralToken> integerLiteralToken = IntegerLiteralToken.tryParse(lexeme, sourcePosition);
        if (integerLiteralToken.isPresent()) {
            return integerLiteralToken;
        }
        return FloatLiteralToken.tryParse(lexeme, sourcePosition);
    }

}
