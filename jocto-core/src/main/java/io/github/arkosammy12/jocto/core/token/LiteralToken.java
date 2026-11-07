package io.github.arkosammy12.jocto.core.token;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;
import io.github.arkosammy12.jocto.core.result.OctoAssemblerException;

public abstract sealed class LiteralToken extends AbstractToken permits NumberLiteralToken, RegisterLiteralToken, StringLiteralToken {

    public LiteralToken(String lexeme, SourcePosition sourcePosition) {
        super(lexeme, sourcePosition);
    }

    public static Optional<? extends LiteralToken> tryParse(String lexeme, SourcePosition sourcePosition) throws OctoAssemblerException {
        Optional<RegisterLiteralToken> registerLiteralToken = RegisterLiteralToken.tryParse(lexeme, sourcePosition);
        if (registerLiteralToken.isPresent()) {
            return registerLiteralToken;
        }
        Optional<StringLiteralToken> stringLiteralToken = StringLiteralToken.tryParse(lexeme, sourcePosition);
        if (stringLiteralToken.isPresent()) {
            return stringLiteralToken;
        }
        return NumberLiteralToken.tryParse(lexeme, sourcePosition);
    }

}
