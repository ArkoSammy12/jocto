package io.github.arkosammy12.jocto.core.token;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.grammar.ComparisonOperatorLexeme;
import io.github.arkosammy12.jocto.core.lexer.SourcePosition;
import io.github.arkosammy12.jocto.core.parser.CalcOperation;
import io.github.arkosammy12.jocto.core.parser.ConditionalOperation;

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
        return "ComparisonOperatorToken[%s, operator=%s]".formatted(this.getBaseStringContents(), this.comparisonOperatorLexeme.name());
    }

    public static Optional<ComparisonOperatorToken> tryPase(String lexeme, SourcePosition sourcePosition) {
        return Token.findMatching(lexeme, ComparisonOperatorLexeme.class, comparisonOperatorLexeme -> new ComparisonOperatorToken(comparisonOperatorLexeme, sourcePosition));
    }

}
