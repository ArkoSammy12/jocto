package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.grammar.CalcOperatorLexeme;
import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.parser.CalcOperation;

import java.util.Optional;

public final class CalcOnlyOperatorToken extends IdentifierToken implements CalcOperatorToken {

    private final CalcOperatorLexeme calcOperatorLexeme;

    public CalcOnlyOperatorToken(CalcOperatorLexeme calcOperatorLexeme, SourcePosition sourcePosition) {
        super(calcOperatorLexeme.getLexeme(), sourcePosition);
        this.calcOperatorLexeme = calcOperatorLexeme;
    }

    @Override
    public CalcOperation getCalcOperation() {
        return this.calcOperatorLexeme.getCalcOperation();
    }

    @Override
    public String toString() {
        return "CalcOperatorToken[%s, operator=%s]".formatted(this.getLexemeAndPositionString(), this.calcOperatorLexeme.name());
    }

    public static Optional<CalcOnlyOperatorToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        return Token.findMatching(lexeme, CalcOperatorLexeme.class, calcOperatorLexeme -> new CalcOnlyOperatorToken(calcOperatorLexeme, sourcePosition));
    }

}
