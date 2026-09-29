package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.parser.InstructionStatementKeyword;

import java.util.Optional;

public final class SemicolonToken extends ReservedNameToken implements InstructionStatementNameToken {

    public SemicolonToken(SourcePosition sourcePosition) {
        super(";", sourcePosition);
    }

    @Override
    public InstructionStatementKeyword getInstructionStatementKeyword() {
        return InstructionStatementKeyword.RETURN;
    }

    @Override
    public String toString() {
        return "SemicolonToken[%s]".formatted(this.getLexemeAndPositionString());
    }

    public static Optional<SemicolonToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        if (";".equals(lexeme)) {
            return Optional.of(new SemicolonToken(sourcePosition));
        } else {
            return Optional.empty();
        }
    }

}
