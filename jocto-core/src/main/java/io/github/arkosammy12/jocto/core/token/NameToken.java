package io.github.arkosammy12.jocto.core.token;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;

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
