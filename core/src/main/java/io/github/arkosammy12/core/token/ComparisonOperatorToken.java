package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.grammar.ComparisonOperatorLexeme;
import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.parser.CalcOperation;
import io.github.arkosammy12.core.parser.ConditionalOperation;

import java.util.Optional;

public final class ComparisonOperatorToken extends ReservedNameToken implements ConditionalOperatorToken, CalcOperatorToken {

    private final ComparisonOperatorLexeme comparisonOperatorLexeme;

    public ComparisonOperatorToken(ComparisonOperatorLexeme comparisonOperatorLexeme, SourcePosition sourcePosition) {
        super(comparisonOperatorLexeme.getLexeme(), sourcePosition);
        this.comparisonOperatorLexeme = comparisonOperatorLexeme;
    }

    @Override
    public ConditionalOperation getConditionalOperation() {
        return this.comparisonOperatorLexeme.getConditionalOperation();
    }

    @Override
    public CalcOperation getCalcOperation() {
        return this.comparisonOperatorLexeme.getCalcOperation();
    }

    @Override
    public String toString() {
        return "ComparisonOperatorToken[%s, operator=%s]".formatted(this.getLexemeAndPositionString(), this.comparisonOperatorLexeme.name());
    }

    public static Optional<ComparisonOperatorToken> tryPase(String lexeme, SourcePosition sourcePosition) {
        return Token.findMatching(lexeme, ComparisonOperatorLexeme.class, comparisonOperatorLexeme -> new ComparisonOperatorToken(comparisonOperatorLexeme, sourcePosition));
    }

}
