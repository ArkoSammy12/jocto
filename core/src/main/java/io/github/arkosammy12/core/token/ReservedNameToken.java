package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public abstract sealed class ReservedNameToken extends NameToken permits AssignmentOnlyKeywordToken, AssignmentOperatorToken, ComparisonOperatorToken, IfBlockKeywordToken, DirectiveToken, IndexRegisterToken, KeyToken, LoopBlockKeywordToken, NonSymbolInstructionStatementKeywordToken, NotKeyToken, SemicolonToken {

    public ReservedNameToken(String lexeme, SourcePosition sourcePosition) {
        super(lexeme, sourcePosition);
    }

    public static Optional<? extends ReservedNameToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        Optional<AssignmentOnlyKeywordToken> assignmentOnlyKeywordToken = AssignmentOnlyKeywordToken.tryParse(lexeme, sourcePosition);
        if (assignmentOnlyKeywordToken.isPresent()) {
            return assignmentOnlyKeywordToken;
        }
        Optional<AssignmentOperatorToken> assignmentOperatorToken = AssignmentOperatorToken.tryParse(lexeme, sourcePosition);
        if (assignmentOperatorToken.isPresent()) {
            return assignmentOperatorToken;
        }
        Optional<IfBlockKeywordToken> conditionalBlockKeywordToken = IfBlockKeywordToken.tryParse(lexeme, sourcePosition);
        if (conditionalBlockKeywordToken.isPresent()) {
            return conditionalBlockKeywordToken;
        }
        Optional<DirectiveToken> directiveToken = DirectiveToken.tryParse(lexeme, sourcePosition);
        if (directiveToken.isPresent()) {
            return directiveToken;
        }
        Optional<KeyToken> keyToken = KeyToken.tryPase(lexeme, sourcePosition);
        if (keyToken.isPresent()) {
            return keyToken;
        }
        Optional<LoopBlockKeywordToken> loopBlockToken = LoopBlockKeywordToken.tryParse(lexeme, sourcePosition);
        if (loopBlockToken.isPresent()) {
            return loopBlockToken;
        }
        Optional<ComparisonOperatorToken> comparisonOperatorToken = ComparisonOperatorToken.tryPase(lexeme, sourcePosition);
        if (comparisonOperatorToken.isPresent()) {
            return comparisonOperatorToken;
        }
        Optional<NonSymbolInstructionStatementKeywordToken> nonSymbolInstructionStatementKeywordToken = NonSymbolInstructionStatementKeywordToken.tryParse(lexeme, sourcePosition);
        if (nonSymbolInstructionStatementKeywordToken.isPresent()) {
            return nonSymbolInstructionStatementKeywordToken;
        }
        Optional<NotKeyToken> notKeyToken = NotKeyToken.tryPase(lexeme, sourcePosition);
        if (notKeyToken.isPresent()) {
            return notKeyToken;
        }
        Optional<IndexRegisterToken> indexRegisterToken = IndexRegisterToken.tryParse(lexeme, sourcePosition);
        if (indexRegisterToken.isPresent()) {
            return indexRegisterToken;
        }
        return SemicolonToken.tryParse(lexeme, sourcePosition);
    }

}
