package io.github.arkosammy12.jocto.core.token;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;
import io.github.arkosammy12.jocto.core.parser.AssignmentKeyword;
import io.github.arkosammy12.jocto.core.parser.ConditionalOperation;

public final class KeyToken extends ReservedNameToken implements AssignmentKeywordToken, ConditionalOperatorToken {

    public KeyToken(SourcePosition sourcePosition) {
        super("key", sourcePosition);
    }

    @Override
    public AssignmentKeyword getAssignmentKeyword() {
        return AssignmentKeyword.KEY;
    }

    @Override
    public ConditionalOperation getConditionalOperation() {
        return ConditionalOperation.KEY_PRESSED;
    }

    @Override
    public String toString() {
        return "KeyToken[%s]".formatted(this.getBaseStringContents());
    }

    public static Optional<KeyToken> tryPase(String lexeme, SourcePosition sourcePosition) {
        if ("key".equals(lexeme)) {
            return Optional.of(new KeyToken(sourcePosition));
        } else {
            return Optional.empty();
        }
    }

}
