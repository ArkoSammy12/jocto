package io.github.arkosammy12.jocto.core.token;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;
import io.github.arkosammy12.jocto.core.parser.InstructionStatementKeyword;

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
