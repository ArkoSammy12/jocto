package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.parser.ConditionalOperation;

import java.util.Optional;

public final class NotKeyToken extends ReservedNameToken implements ConditionalOperatorToken {

    public NotKeyToken(SourcePosition sourcePosition) {
        super("-key", sourcePosition);
    }

    @Override
    public ConditionalOperation getConditionalOperation() {
        return ConditionalOperation.KEY_NOT_PRESSED;
    }

    @Override
    public String toString() {
        return "NotKeyToken[%s]".formatted(this.getBaseStringContents());
    }

    public static Optional<NotKeyToken> tryPase(String lexeme, SourcePosition sourcePosition) {
        if ("-key".equals(lexeme)) {
            return Optional.of(new NotKeyToken(sourcePosition));
        } else {
            return Optional.empty();
        }
    }

}
