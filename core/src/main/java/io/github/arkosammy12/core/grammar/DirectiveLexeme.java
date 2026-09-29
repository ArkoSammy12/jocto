package io.github.arkosammy12.core.grammar;

import io.github.arkosammy12.core.lexer.Lexeme;

public enum DirectiveLexeme implements Lexeme {
    LABEL_DEFINITION(":"),
    NEXT(":next"),
    UNPACK(":unpack"),
    BREAKPOINT(":breakpoint"),
    PROTO(":proto"),
    ALIAS(":alias"),
    CONST(":const"),
    ORG(":org"),
    MACRO(":macro"),
    CALC(":calc"),
    BYTE(":byte"),
    CALL(":call"),
    STRING_MODE(":stringmode"),
    ASSERT(":assert"),
    MONITOR(":monitor"),
    POINTER(":pointer");

    private final String lexeme;

    DirectiveLexeme(String lexeme) {
        this.lexeme = lexeme;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

}
