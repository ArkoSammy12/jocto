package io.github.arkosammy12.jocto.core.token;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;
import io.github.arkosammy12.jocto.core.parser.ConditionalOperation;

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
