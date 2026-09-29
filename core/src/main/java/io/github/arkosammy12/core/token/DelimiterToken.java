package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.grammar.Delimiter;
import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public final class DelimiterToken extends IdentifierToken {

    private final Delimiter delimiter;
    private final Delimiter.Type delimiterType;

    public DelimiterToken(Delimiter delimiter, Delimiter.Type delimiterType, SourcePosition sourcePosition) {
        super(switch (delimiter) {
            case BRACE -> switch (delimiterType) {
                case OPENING -> "{";
                case CLOSING -> "}";
            };
            case PARENTHESIS -> switch (delimiterType) {
                case OPENING -> "(";
                case CLOSING -> ")";
            };
        }, sourcePosition);
        this.delimiter = delimiter;
        this.delimiterType = delimiterType;
    }

    public Delimiter getDelimiter() {
        return this.delimiter;
    }

    public Delimiter.Type getDelimiterType() {
        return this.delimiterType;
    }

    @Override
    public String toString() {
        return "DelimiterToken[%s, delimiter=%s, type=%s]".formatted(this.getLexemeAndPositionString(), this.delimiter.name(), this.delimiterType.name());
    }

    public static Optional<DelimiterToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        return Optional.ofNullable(switch (lexeme) {
            case "{" -> new DelimiterToken(Delimiter.BRACE, Delimiter.Type.OPENING, sourcePosition);
            case "}" -> new DelimiterToken(Delimiter.BRACE, Delimiter.Type.CLOSING, sourcePosition);
            case "(" -> new DelimiterToken(Delimiter.PARENTHESIS, Delimiter.Type.OPENING, sourcePosition);
            case ")" -> new DelimiterToken(Delimiter.PARENTHESIS, Delimiter.Type.CLOSING, sourcePosition);
            default -> null;
        });
    }

}
