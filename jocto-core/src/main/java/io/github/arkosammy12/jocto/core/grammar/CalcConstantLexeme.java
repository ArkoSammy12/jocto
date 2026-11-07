package io.github.arkosammy12.jocto.core.grammar;

import io.github.arkosammy12.jocto.core.lexer.Lexeme;

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
