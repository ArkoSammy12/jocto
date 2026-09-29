package io.github.arkosammy12.core.grammar;

import io.github.arkosammy12.core.lexer.Lexeme;

public enum ConditionalBlockKeywordLexeme implements Lexeme {
    IF("if"),
    THEN("then"),
    BEGIN("begin"),
    ELSE("else"),
    END("end");

    private final String lexeme;

    ConditionalBlockKeywordLexeme(String lexeme) {
        this.lexeme = lexeme;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

}
