package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public abstract sealed class NameToken extends AbstractToken permits IdentifierToken, ReservedNameToken {

    public NameToken(String lexeme, SourcePosition sourcePosition) {
        super(lexeme, sourcePosition);
    }

    public static Optional<? extends NameToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        Optional<? extends ReservedNameToken> reservedNameToken = ReservedNameToken.tryParse(lexeme, sourcePosition);
        if (reservedNameToken.isPresent()) {
            return reservedNameToken;
        }
        return IdentifierToken.tryParse(lexeme, sourcePosition);
    }


}
