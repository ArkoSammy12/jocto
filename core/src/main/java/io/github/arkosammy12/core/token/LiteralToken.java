package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public abstract sealed class LiteralToken extends AbstractToken permits FloatLiteralToken, IntegerLiteralToken, RegisterLiteralToken, StringLiteralToken {

    public LiteralToken(String lexeme, SourcePosition sourcePosition) {
        super(lexeme, sourcePosition);
    }

    public static Optional<? extends LiteralToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        Optional<RegisterLiteralToken> registerLiteralToken = RegisterLiteralToken.tryParse(lexeme, sourcePosition);
        if (registerLiteralToken.isPresent()) {
            return registerLiteralToken;
        }
        Optional<StringLiteralToken> stringLiteralToken = StringLiteralToken.tryParse(lexeme, sourcePosition);
        if (stringLiteralToken.isPresent()) {
            return stringLiteralToken;
        }
        Optional<IntegerLiteralToken> integerLiteralToken = IntegerLiteralToken.tryParse(lexeme, sourcePosition);
        if (integerLiteralToken.isPresent()) {
            return integerLiteralToken;
        }
        return FloatLiteralToken.tryParse(lexeme, sourcePosition);
    }

}
