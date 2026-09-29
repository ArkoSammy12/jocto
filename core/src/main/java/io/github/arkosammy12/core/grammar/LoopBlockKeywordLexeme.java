package io.github.arkosammy12.core.grammar;

import io.github.arkosammy12.core.lexer.Lexeme;

public enum LoopBlockKeywordLexeme implements Lexeme {
    LOOP("loop"),
    WHILE("while"),
    AGAIN("again");

    private final String lexeme;

    LoopBlockKeywordLexeme(String lexeme) {
        this.lexeme = lexeme;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

}
