package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.grammar.StringModeArgument;
import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public final class StringModeArgumentToken extends IdentifierToken {

    private final StringModeArgument stringModeArgument;

    public StringModeArgumentToken(StringModeArgument stringModeArgument, SourcePosition sourcePosition) {
        super(stringModeArgument.getLexeme(), sourcePosition);
        this.stringModeArgument = stringModeArgument;
    }

    public StringModeArgument getStringModeArgument() {
        return this.stringModeArgument;
    }

    @Override
    public String toString() {
        return "StringModeArgumentIdentifierToken[%s, stringModeArgument=%s]".formatted(this.getBaseStringContents(), this.stringModeArgument.name());
    }

    public static Optional<StringModeArgumentToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        return Token.findMatching(lexeme, StringModeArgument.class, stringModeArgument -> new StringModeArgumentToken(stringModeArgument, sourcePosition));
    }

}
