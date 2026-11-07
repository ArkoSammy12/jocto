package io.github.arkosammy12.jocto.core.grammar;

import io.github.arkosammy12.jocto.core.lexer.Lexeme;

public enum IfBlockKeywordLexeme implements Lexeme {
    IF("if"),
    THEN("then"),
    BEGIN("begin"),
    ELSE("else"),
    END("end");

    private final String lexeme;

    IfBlockKeywordLexeme(String lexeme) {
        this.lexeme = lexeme;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

}
