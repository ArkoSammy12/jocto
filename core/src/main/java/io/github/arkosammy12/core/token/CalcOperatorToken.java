package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.parser.CalcOperation;

import java.util.Optional;

public sealed interface CalcOperatorToken extends Token permits CalcOnlyOperatorToken, ComparisonOperatorToken, DashToken {

    CalcOperation getCalcOperation();

    static Optional<? extends CalcOperatorToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        Optional<DashToken> dashToken = DashToken.tryPase(lexeme, sourcePosition);
        if (dashToken.isPresent()) {
            return dashToken;
        }
        Optional<ComparisonOperatorToken> comparisonOperatorToken = ComparisonOperatorToken.tryPase(lexeme, sourcePosition);
        if (comparisonOperatorToken.isPresent()) {
            return comparisonOperatorToken;
        }
        return CalcOnlyOperatorToken.tryParse(lexeme, sourcePosition);
    }

}
