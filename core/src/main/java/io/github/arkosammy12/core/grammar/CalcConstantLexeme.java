package io.github.arkosammy12.core.grammar;

import io.github.arkosammy12.core.lexer.Lexeme;

public enum CalcConstantLexeme implements Lexeme {
    E("E"),
    PI("PI"),
    HERE("HERE");

    private final String lexeme;

    CalcConstantLexeme(String lexeme) {
        this.lexeme = lexeme;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

}
