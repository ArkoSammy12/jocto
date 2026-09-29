package io.github.arkosammy12.core.grammar;

import io.github.arkosammy12.core.lexer.Lexeme;

public enum StringModeArgument implements Lexeme {
    CHAR("CHAR"),
    INDEX("INDEX"),
    VALUE("VALUE");

    private final String lexeme;

    StringModeArgument(String lexeme) {
        this.lexeme = lexeme;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

}
