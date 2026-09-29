package io.github.arkosammy12.core.grammar;

import io.github.arkosammy12.core.lexer.Lexeme;
import io.github.arkosammy12.core.parser.InstructionStatementKeyword;

public enum InstructionStatementLexeme implements Lexeme {
    RETURN("return", InstructionStatementKeyword.RETURN),
    CLEAR("clear", InstructionStatementKeyword.CLEAR),
    BCD("bcd", InstructionStatementKeyword.BCD),
    SAVE("save", InstructionStatementKeyword.SAVE),
    LOAD("load", InstructionStatementKeyword.LOAD),
    SPRITE("sprite", InstructionStatementKeyword.SPRITE),
    JUMP("jump", InstructionStatementKeyword.JUMP),
    JUMP0("jump0", InstructionStatementKeyword.JUMP0),
    HIRES("hires", InstructionStatementKeyword.HIRES),
    LORES("lores", InstructionStatementKeyword.LORES),
    SCROLL_DOWN("scroll-down", InstructionStatementKeyword.SCROLL_DOWN),
    SCROLL_LEFT("scroll-left", InstructionStatementKeyword.SCROLL_LEFT),
    SCROLL_RIGHT("scroll-right", InstructionStatementKeyword.SCROLL_RIGHT),
    EXIT("exit", InstructionStatementKeyword.EXIT),
    SAVE_FLAGS("saveflags", InstructionStatementKeyword.SAVE_FLAGS),
    LOAD_FLAGS("loadflags", InstructionStatementKeyword.LOAD_FLAGS),
    PLANE("plane", InstructionStatementKeyword.PLANE),
    AUDIO("audio", InstructionStatementKeyword.AUDIO),
    SCROLL_UP("scroll-up", InstructionStatementKeyword.SCROLL_UP);

    private final String lexeme;
    private final InstructionStatementKeyword instructionStatementKeyword;

    InstructionStatementLexeme(String lexeme, InstructionStatementKeyword instructionStatementKeyword) {
        this.lexeme = lexeme;
        this.instructionStatementKeyword = instructionStatementKeyword;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

    public InstructionStatementKeyword getInstructionStatementKeyword() {
        return this.instructionStatementKeyword;
    }

}
