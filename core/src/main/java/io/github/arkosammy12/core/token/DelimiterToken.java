package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.grammar.Delimiter;
import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public final class DelimiterToken extends IdentifierToken {

    private final Delimiter delimiter;

    public DelimiterToken(Delimiter delimiter, SourcePosition sourcePosition) {
        super(switch (delimiter) {
            case OPENING_PARENTHESIS -> "(";
            case CLOSING_PARENTHESIS -> ")";
            case OPENING_BRACE -> "{";
            case CLOSING_BRACE -> "}";
        }, sourcePosition);
        this.delimiter = delimiter;
    }

    public Delimiter getDelimiter() {
        return this.delimiter;
    }

    @Override
    public String toString() {
        return "DelimiterToken[%s, delimiter=%s]".formatted(this.getBaseStringContents(), this.delimiter.name());
    }

    public static Optional<DelimiterToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        return Optional.ofNullable(switch (lexeme) {
            case "{" -> new DelimiterToken(Delimiter.OPENING_BRACE, sourcePosition);
            case "}" -> new DelimiterToken(Delimiter.CLOSING_BRACE, sourcePosition);
            case "(" -> new DelimiterToken(Delimiter.OPENING_PARENTHESIS, sourcePosition);
            case ")" -> new DelimiterToken(Delimiter.CLOSING_PARENTHESIS, sourcePosition);
            default -> null;
        });
    }

}
