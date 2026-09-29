package io.github.arkosammy12.core.grammar;

import io.github.arkosammy12.core.lexer.Lexeme;
import io.github.arkosammy12.core.parser.CalcOperation;

public enum CalcOperatorLexeme implements Lexeme {
    BITWISE_NOT("~", CalcOperation.BITWISE_NOT),
    LOGICAL_NOT("!", CalcOperation.LOGICAL_NOT),
    SIN("sin", CalcOperation.SIN),
    COS("cos", CalcOperation.COS),
    TAN("tan", CalcOperation.TAN),
    EXP("exp", CalcOperation.EXP),
    LOG("log", CalcOperation.LOG),
    ABS("abs", CalcOperation.ABS),
    SQRT("sqrt", CalcOperation.SQRT),
    SIGN("sign", CalcOperation.SIN),
    CEIL("ceil", CalcOperation.CEIL),
    FLOOR("floor", CalcOperation.FLOOR),
    AT("@", CalcOperation.AT),
    STRLEN("strlen", CalcOperation.STRLEN),
    PLUS("+", CalcOperation.PLUS),
    MULTIPLY("*", CalcOperation.MULTIPLY),
    DIVIDE("/", CalcOperation.DIVIDE),
    MODULO("%", CalcOperation.MODULO),
    BITWISE_AND("&", CalcOperation.BITWISE_AND),
    BITWISE_OR("|", CalcOperation.BITWISE_OR),
    BITWISE_XOR("^", CalcOperation.BITWISE_XOR),
    LEFT_SHIFT("<<", CalcOperation.LEFT_SHIFT),
    RIGHT_SHIFT(">>", CalcOperation.RIGHT_SHIFT),
    POW("pow", CalcOperation.POW),
    MIN("min", CalcOperation.MIN),
    MAX("max", CalcOperation.MAX);

    private final String lexeme;
    private final CalcOperation calcOperation;

    CalcOperatorLexeme(String lexeme, CalcOperation calcOperation) {
        this.lexeme = lexeme;
        this.calcOperation = calcOperation;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

    public CalcOperation getCalcOperation() {
        return this.calcOperation;
    }

}
