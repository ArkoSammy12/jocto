package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.Lexeme;
import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;
import java.util.function.Function;

public sealed interface Token permits AbstractToken, AssignmentKeywordToken, CalcOperatorToken, ConditionalOperatorToken, InstructionStatementNameToken {

    String getLexeme();

    SourcePosition getSourcePosition();

    static <E extends Enum<E> & Lexeme, T extends AbstractToken> Optional<T> findMatching(String lexeme, Class<E> enumClass, Function<E, T> tokenConstructor) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.getLexeme().equals(lexeme)) {
                return Optional.of(tokenConstructor.apply(e));
            }
        }
        return Optional.empty();
    }

    static Token tryParse(String lexeme, SourcePosition sourcePosition) {
        Optional<? extends AbstractToken> abstractToken = AbstractToken.tryParse(lexeme, sourcePosition);
        if (abstractToken.isPresent()) {
            return abstractToken.get();
        }
        Optional<? extends AssignmentKeywordToken> assignmentKeywordToken = AssignmentKeywordToken.tryParse(lexeme, sourcePosition);
        if (assignmentKeywordToken.isPresent()) {
            return assignmentKeywordToken.get();
        }
        Optional<? extends CalcOperatorToken> calcOperatorToken = CalcOperatorToken.tryParse(lexeme, sourcePosition);
        if (calcOperatorToken.isPresent()) {
            return calcOperatorToken.get();
        }
        Optional<? extends ConditionalOperatorToken> conditionalOperatorToken = ConditionalOperatorToken.tryParse(lexeme, sourcePosition);
        if (conditionalOperatorToken.isPresent()) {
            return conditionalOperatorToken.get();
        }
        Optional<? extends InstructionStatementNameToken> instructionStatementNameToken = InstructionStatementNameToken.tryParse(lexeme, sourcePosition);
        if (instructionStatementNameToken.isPresent()) {
            return instructionStatementNameToken.get();
        }
        return new IdentifierToken(lexeme, sourcePosition);
    }

}
