package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.Lexeme;
import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public abstract sealed class AbstractToken implements Token permits LiteralToken, NameToken {

    private final String lexeme;
    private final SourcePosition sourcePosition;

    public AbstractToken(String lexeme, SourcePosition sourcePosition) {
        if (lexeme.isBlank()) {
            throw new IllegalArgumentException("Cannot create blank token!");
        }
        if (!this.allowsWhitespace() && Lexeme.containsWhitespace(lexeme)) {
            throw new IllegalArgumentException("Cannot create token '%s' because it contains whitespace!".formatted(lexeme));
        }
        this.lexeme = lexeme;
        this.sourcePosition = sourcePosition;
    }

    protected boolean allowsWhitespace() {
        return false;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

    @Override
    public SourcePosition getSourcePosition() {
        return this.sourcePosition;
    }

    public static Optional<? extends AbstractToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        Optional<? extends NameToken> nameToken = NameToken.tryParse(lexeme, sourcePosition);
        if (nameToken.isPresent()) {
            return nameToken;
        }
        return LiteralToken.tryParse(lexeme, sourcePosition);
    }

    @Override
    public String toString() {
        return "Token[%s]".formatted(this.getLexemeAndPositionString());
    }

    protected String getLexemeAndPositionString() {
        return "lexeme='%s', position=%s".formatted(this.lexeme, this.sourcePosition);
    }

}
