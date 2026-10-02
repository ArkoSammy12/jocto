package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.grammar.AssignmentOperatorLexeme;
import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.parser.AssignmentOperation;

import java.util.Optional;

public final class AssignmentOperatorToken extends ReservedNameToken {

    private final AssignmentOperatorLexeme assignmentOperatorLexeme;

    public AssignmentOperatorToken(AssignmentOperatorLexeme assignmentOperatorLexeme, SourcePosition sourcePosition) {
        super(assignmentOperatorLexeme.getLexeme(), sourcePosition);
        this.assignmentOperatorLexeme = assignmentOperatorLexeme;
    }

    public AssignmentOperation getAssignmentOperation() {
        return this.assignmentOperatorLexeme.getAssignmentOperation();
    }

    @Override
    public String toString() {
        return "AssignmentOperatorToken[%s, operator=%s]".formatted(this.getBaseStringContents(), this.assignmentOperatorLexeme.name());
    }

    public static Optional<AssignmentOperatorToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        return Token.findMatching(lexeme, AssignmentOperatorLexeme.class, assignmentOperatorLexeme -> new AssignmentOperatorToken(assignmentOperatorLexeme, sourcePosition));
    }

}