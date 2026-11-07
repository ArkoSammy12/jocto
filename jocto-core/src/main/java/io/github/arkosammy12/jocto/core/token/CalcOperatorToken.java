package io.github.arkosammy12.jocto.core.token;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;
import io.github.arkosammy12.jocto.core.parser.CalcOperation;

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
