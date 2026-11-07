package io.github.arkosammy12.jocto.core.grammar;

import io.github.arkosammy12.jocto.core.lexer.Lexeme;

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
