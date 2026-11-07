package io.github.arkosammy12.jocto.core.grammar;

import io.github.arkosammy12.jocto.core.lexer.Lexeme;

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
