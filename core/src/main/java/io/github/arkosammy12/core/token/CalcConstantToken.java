package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.grammar.CalcConstantLexeme;
import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public final class CalcConstantToken extends IdentifierToken {

    private final CalcConstantLexeme calcConstantLexeme;

    public CalcConstantToken(CalcConstantLexeme calcConstantLexeme, SourcePosition sourcePosition) {
        super(calcConstantLexeme.getLexeme(), sourcePosition);
        this.calcConstantLexeme = calcConstantLexeme;
    }

    public CalcConstantLexeme getCalcConstant() {
        return this.calcConstantLexeme;
    }

    @Override
    public String toString() {
        return "CalcConstantIdentifierToken[%s, constant=%s]".formatted(this.getLexemeAndPositionString(), this.calcConstantLexeme.name());
    }

    public static Optional<CalcConstantToken> tryParser(String lexeme, SourcePosition sourcePosition) {
        return Token.findMatching(lexeme, CalcConstantLexeme.class, calcConstantLexeme -> new CalcConstantToken(calcConstantLexeme, sourcePosition));
    }

}