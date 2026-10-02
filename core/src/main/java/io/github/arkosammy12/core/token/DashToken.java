package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.parser.AssignmentKeyword;
import io.github.arkosammy12.core.parser.CalcOperation;

import java.util.Optional;

public non-sealed class DashToken extends IdentifierToken implements AssignmentKeywordToken, CalcOperatorToken {

    public DashToken(SourcePosition sourcePosition) {
        super("-", sourcePosition);
    }

    @Override
    public AssignmentKeyword getAssignmentKeyword() {
        return AssignmentKeyword.SAVE_LOAD_RANGE_SEPARATOR;
    }

    @Override
    public CalcOperation getCalcOperation() {
        return CalcOperation.MINUS;
    }

    @Override
    public String toString() {
        return "DashToken[%s]".formatted(this.getBaseStringContents());
    }

    public static Optional<DashToken> tryPase(String lexeme, SourcePosition sourcePosition) {
        if ("-".equals(lexeme)) {
            return Optional.of(new DashToken(sourcePosition));
        } else {
            return Optional.empty();
        }
    }

}
