package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.grammar.InstructionStatementLexeme;
import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.parser.InstructionStatementKeyword;

import java.util.Optional;

public final class NonSymbolInstructionStatementKeywordToken extends ReservedNameToken implements InstructionStatementNameToken {

    private final InstructionStatementLexeme instructionStatementLexeme;

    public NonSymbolInstructionStatementKeywordToken(InstructionStatementLexeme instructionStatementLexeme, SourcePosition sourcePosition) {
        super(instructionStatementLexeme.getLexeme(), sourcePosition);
        this.instructionStatementLexeme = instructionStatementLexeme;
    }

    @Override
    public InstructionStatementKeyword getInstructionStatementKeyword() {
        return this.instructionStatementLexeme.getInstructionStatementKeyword();
    }

    @Override
    public String toString() {
        return "InstructionStatementKeywordToken[%s, keyword=%s]".formatted(this.getBaseStringContents(), this.instructionStatementLexeme.name());
    }

    public static Optional<NonSymbolInstructionStatementKeywordToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        return Token.findMatching(lexeme, InstructionStatementLexeme.class, instructionStatementLexeme -> new NonSymbolInstructionStatementKeywordToken(instructionStatementLexeme, sourcePosition));
    }

}
