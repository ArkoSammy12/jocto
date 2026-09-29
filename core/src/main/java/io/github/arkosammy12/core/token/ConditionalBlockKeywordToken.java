package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.grammar.ConditionalBlockKeywordLexeme;
import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public final class ConditionalBlockKeywordToken extends ReservedNameToken {

    private final ConditionalBlockKeywordLexeme conditionalBlockKeywordLexeme;

    public ConditionalBlockKeywordToken(ConditionalBlockKeywordLexeme conditionalBlockKeywordLexeme, SourcePosition sourcePosition) {
        super(conditionalBlockKeywordLexeme.getLexeme(), sourcePosition);
        this.conditionalBlockKeywordLexeme = conditionalBlockKeywordLexeme;
    }

    public ConditionalBlockKeywordLexeme getConditionalBlockKeyword() {
        return this.conditionalBlockKeywordLexeme;
    }

    @Override
    public String toString() {
        return "ConditionalBlockKeywordToken[%s, keyword=%s]".formatted(this.getLexemeAndPositionString(), this.conditionalBlockKeywordLexeme.name());
    }

    public static Optional<ConditionalBlockKeywordToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        return Token.findMatching(lexeme, ConditionalBlockKeywordLexeme.class, conditionalBlockKeywordLexeme -> new ConditionalBlockKeywordToken(conditionalBlockKeywordLexeme, sourcePosition));
    }

}
