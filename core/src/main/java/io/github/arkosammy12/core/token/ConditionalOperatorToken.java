package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.parser.ConditionalOperation;

import java.util.Optional;

public sealed interface ConditionalOperatorToken extends Token permits KeyToken, ComparisonOperatorToken, NotKeyToken {

    ConditionalOperation getConditionalOperation();

    static Optional<? extends ConditionalOperatorToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        Optional<KeyToken> keyToken = KeyToken.tryPase(lexeme, sourcePosition);
        if (keyToken.isPresent()) {
            return keyToken;
        }
        Optional<NotKeyToken> notKeyToken = NotKeyToken.tryPase(lexeme, sourcePosition);
        if (notKeyToken.isPresent()) {
            return notKeyToken;
        }
        return ComparisonOperatorToken.tryPase(lexeme, sourcePosition);
    }

}
