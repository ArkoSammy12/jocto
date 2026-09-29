package io.github.arkosammy12.core.grammar;

import io.github.arkosammy12.core.lexer.Lexeme;
import io.github.arkosammy12.core.parser.AssignmentOperation;

public enum AssignmentOperatorLexeme implements Lexeme {
    SET(":=", AssignmentOperation.SET),
    INCREMENT("+=", AssignmentOperation.ADD),
    LEFT_SUBTRACT("-=", AssignmentOperation.LEFT_SUBTRACT),
    RIGHT_SUBTRACT("=-", AssignmentOperation.RIGHT_SUBTRACT),
    BITWISE_OR("|=", AssignmentOperation.BITWISE_OR),
    BITWISE_AND("&=", AssignmentOperation.BITWISE_AND),
    BITWISE_XOR("^=", AssignmentOperation.BITWISE_XOR),
    RIGHT_SHIFT(">>=", AssignmentOperation.RIGHT_SHIFT),
    LEFT_SHIFT("<<=", AssignmentOperation.LEFT_SHIFT);

    private final String lexeme;
    private final AssignmentOperation assignmentOperation;

    AssignmentOperatorLexeme(String lexeme, AssignmentOperation assignmentOperation) {
        this.lexeme = lexeme;
        this.assignmentOperation = assignmentOperation;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

    public AssignmentOperation getAssignmentOperation() {
        return this.assignmentOperation;
    }

}
