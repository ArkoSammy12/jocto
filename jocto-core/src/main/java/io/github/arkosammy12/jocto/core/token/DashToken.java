package io.github.arkosammy12.jocto.core.token;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;
import io.github.arkosammy12.jocto.core.parser.AssignmentKeyword;
import io.github.arkosammy12.jocto.core.parser.CalcOperation;

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
