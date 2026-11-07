package io.github.arkosammy12.jocto.core.grammar;

import io.github.arkosammy12.jocto.core.lexer.Lexeme;
import io.github.arkosammy12.jocto.core.parser.AssignmentKeyword;

public enum AssignmentKeywordLexeme implements Lexeme {
    DELAY("delay", AssignmentKeyword.DELAY),
    BUZZER("buzzer", AssignmentKeyword.BUZZER),
    HEX("hex", AssignmentKeyword.HEX),
    RANDOM("random", AssignmentKeyword.RANDOM),
    BIGHEX("bighex", AssignmentKeyword.BIGHEX),
    LONG("long", AssignmentKeyword.LONG),
    PITCH("pitch", AssignmentKeyword.PITCH);

    private final String lexeme;
    private final AssignmentKeyword assignmentKeyword;

    AssignmentKeywordLexeme(String lexeme, AssignmentKeyword assignmentKeyword) {
        this.lexeme = lexeme;
        this.assignmentKeyword = assignmentKeyword;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

    public AssignmentKeyword getAssignmentKeyword() {
        return this.assignmentKeyword;
    }

}
