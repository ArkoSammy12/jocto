package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.result.OctoAssemblerException;

import java.util.Optional;

public final class StringLiteralToken extends LiteralToken {

    private final String resolved;

    public StringLiteralToken(String lexeme, String resolved, SourcePosition sourcePosition) {
        super(lexeme, sourcePosition);
        this.resolved = resolved;
    }

    @Override
    protected boolean allowsWhitespace() {
        return true;
    }

    public String getResolved() {
        return this.resolved;
    }

    @Override
    public String toString() {
        return "StringLiteralToken[%s, resolved=%s]".formatted(this.getBaseStringContents(), this.resolved);
    }

    public static Optional<StringLiteralToken> tryParse(String lexeme, SourcePosition sourcePosition) throws OctoAssemblerException {
        if (lexeme.length() < 2) {
            return Optional.empty();
        }
        if (lexeme.charAt(0) == '"' && lexeme.charAt(lexeme.length() - 1) == '"') {
            String contents = lexeme.substring(1, lexeme.length() - 1);
            StringBuilder resolved = new StringBuilder();

            for (int i = 0; i < contents.length(); i++) {
                char c = contents.charAt(i);
                if (c == '\\') {
                    i++;
                    if (i >= contents.length()) {
                        throw new OctoAssemblerException("Unterminated escape sequence in string literal!", sourcePosition);
                    } else {
                        switch (contents.charAt(i)) {
                            case 't' -> resolved.append('\t');
                            case 'n' -> resolved.append('\n');
                            case 'r' -> resolved.append('\r');
                            case 'v' -> resolved.append('\u000b');
                            case '0' -> resolved.append('\0');
                            case '\\' -> resolved.append('\\');
                            case '"' -> resolved.append('"');
                            default -> throw new OctoAssemblerException("Unrecognized escape character '%s' in string literal!".formatted(contents.charAt(i)), sourcePosition);
                        }
                    }
                } else {
                    resolved.append(c);
                }
            }

            return Optional.of(new StringLiteralToken(lexeme, resolved.toString(), sourcePosition));
        } else {
            return Optional.empty();
        }
    }

}
