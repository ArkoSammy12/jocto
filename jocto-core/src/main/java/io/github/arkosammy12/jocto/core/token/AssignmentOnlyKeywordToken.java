package io.github.arkosammy12.jocto.core.token;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.grammar.AssignmentKeywordLexeme;
import io.github.arkosammy12.jocto.core.lexer.SourcePosition;
import io.github.arkosammy12.jocto.core.parser.AssignmentKeyword;

public final class AssignmentOnlyKeywordToken extends ReservedNameToken implements AssignmentKeywordToken {

    private final AssignmentKeywordLexeme assignmentKeywordLexeme;

    public AssignmentOnlyKeywordToken(AssignmentKeywordLexeme assignmentKeywordLexeme, SourcePosition sourcePosition) {
        super(assignmentKeywordLexeme.getLexeme(), sourcePosition);
        this.assignmentKeywordLexeme = assignmentKeywordLexeme;
    }

    @Override
    public AssignmentKeyword getAssignmentKeyword() {
        return this.assignmentKeywordLexeme.getAssignmentKeyword();
    }

    @Override
    public String toString() {
        return "AssignmentKeywordToken[%s, keyword=%s]".formatted(this.getBaseStringContents(), this.assignmentKeywordLexeme.name());
    }

    public static Optional<AssignmentOnlyKeywordToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        return Token.findMatching(lexeme, AssignmentKeywordLexeme.class, assignmentKeywordLexeme -> new AssignmentOnlyKeywordToken(assignmentKeywordLexeme, sourcePosition));
    }

}
