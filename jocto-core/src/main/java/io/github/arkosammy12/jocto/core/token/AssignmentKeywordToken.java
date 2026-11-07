package io.github.arkosammy12.jocto.core.token;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;
import io.github.arkosammy12.jocto.core.parser.AssignmentKeyword;

public sealed interface AssignmentKeywordToken extends Token permits AssignmentOnlyKeywordToken, DashToken, KeyToken {

    AssignmentKeyword getAssignmentKeyword();

    static Optional<? extends AssignmentKeywordToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        Optional<KeyToken> keyToken = KeyToken.tryPase(lexeme, sourcePosition);
        if (keyToken.isPresent()) {
            return keyToken;
        }
        Optional<DashToken> dashToken = DashToken.tryPase(lexeme, sourcePosition);
        if (dashToken.isPresent()) {
            return dashToken;
        }
        return AssignmentOnlyKeywordToken.tryParse(lexeme, sourcePosition);
    }

}
