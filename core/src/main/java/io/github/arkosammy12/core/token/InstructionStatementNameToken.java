package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.parser.InstructionStatementKeyword;

import java.util.Optional;

public sealed interface InstructionStatementNameToken extends Token permits
        NonSymbolInstructionStatementKeywordToken,
        SemicolonToken {

    InstructionStatementKeyword getInstructionStatementKeyword();

    static Optional<? extends InstructionStatementNameToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        Optional<SemicolonToken> semicolonToken = SemicolonToken.tryParse(lexeme, sourcePosition);
        if (semicolonToken.isPresent()) {
            return semicolonToken;
        }
        return NonSymbolInstructionStatementKeywordToken.tryParse(lexeme, sourcePosition);
    }

}
