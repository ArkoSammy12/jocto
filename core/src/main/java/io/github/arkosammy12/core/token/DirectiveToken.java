package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.grammar.DirectiveLexeme;
import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public final class DirectiveToken extends ReservedNameToken {

    private final DirectiveLexeme directiveLexeme;

    public DirectiveToken(DirectiveLexeme directiveLexeme, SourcePosition sourcePosition) {
        super(directiveLexeme.getLexeme(), sourcePosition);
        this.directiveLexeme = directiveLexeme;
    }

    public DirectiveLexeme getDirective() {
        return this.directiveLexeme;
    }

    @Override
    public String toString() {
        return "DirectiveToken[%s, directive=%s]".formatted(this.getLexemeAndPositionString(), this.directiveLexeme.name());
    }

    public static Optional<DirectiveToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        return Token.findMatching(lexeme, DirectiveLexeme.class, directiveLexeme -> new DirectiveToken(directiveLexeme, sourcePosition));
    }

}
