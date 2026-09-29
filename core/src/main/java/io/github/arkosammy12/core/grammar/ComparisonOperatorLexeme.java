package io.github.arkosammy12.core.grammar;

import io.github.arkosammy12.core.lexer.Lexeme;
import io.github.arkosammy12.core.parser.CalcOperation;
import io.github.arkosammy12.core.parser.ConditionalOperation;

public enum ComparisonOperatorLexeme implements Lexeme {
    EQUALS("==", ConditionalOperation.EQUALITY, CalcOperation.EQUALS),
    NOT_EQUALS("!=", ConditionalOperation.INEQUALITY, CalcOperation.NOT_EQUALS),
    LESS_THAN("<", ConditionalOperation.LESS_THAN, CalcOperation.LESS_THAN),
    GREATER_THAN(">", ConditionalOperation.GREATER_THAN, CalcOperation.GREATER_THAN),
    LESS_THAN_OR_EQUALS_TO("<=", ConditionalOperation.LESS_THAN_OR_EQUALS, CalcOperation.LESS_THAN_THAN_OR_EQUALS),
    GREATER_THAN_OR_EQUALS_TO(">=", ConditionalOperation.GREATER_THAN_OR_EQUALS, CalcOperation.GREATER_THAN_OR_EQUALS);

    private final String lexeme;
    private final ConditionalOperation conditionalOperation;
    private final CalcOperation calcOperation;

    ComparisonOperatorLexeme(String lexeme, ConditionalOperation conditionalOperation, CalcOperation calcOperation) {
        this.lexeme = lexeme;
        this.conditionalOperation = conditionalOperation;
        this.calcOperation = calcOperation;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

    public ConditionalOperation getConditionalOperation() {
        return this.conditionalOperation;
    }

    public CalcOperation getCalcOperation() {
        return this.calcOperation;
    }

}
