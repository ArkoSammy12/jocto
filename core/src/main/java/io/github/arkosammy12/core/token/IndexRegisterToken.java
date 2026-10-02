package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public final class IndexRegisterToken extends ReservedNameToken {

    public IndexRegisterToken(SourcePosition sourcePosition) {
        super("i", sourcePosition);
    }

    @Override
    public String toString() {
        return "IndexRegisterToken[%s]".formatted(this.getBaseStringContents());
    }

    public static Optional<IndexRegisterToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        if ("i".equals(lexeme)) {
            return Optional.of(new IndexRegisterToken(sourcePosition));
        } else {
            return Optional.empty();
        }
    }

}
