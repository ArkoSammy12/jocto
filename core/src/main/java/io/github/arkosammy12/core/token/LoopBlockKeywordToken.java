package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.grammar.LoopBlockKeywordLexeme;
import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public final class LoopBlockKeywordToken extends ReservedNameToken {

    private final LoopBlockKeywordLexeme loopBlockKeywordLexeme;

    public LoopBlockKeywordToken(LoopBlockKeywordLexeme loopBlockKeywordLexeme, SourcePosition sourcePosition) {
        super(loopBlockKeywordLexeme.getLexeme(), sourcePosition);
        this.loopBlockKeywordLexeme = loopBlockKeywordLexeme;
    }

    public LoopBlockKeywordLexeme getLoopBlockKeyword() {
        return this.loopBlockKeywordLexeme;
    }

    @Override
    public String toString() {
        return "LoopBlockKeywordToken[%s, keyword=%s]".formatted(this.getBaseStringContents(), this.loopBlockKeywordLexeme.name());
    }

    public static Optional<LoopBlockKeywordToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        return Token.findMatching(lexeme, LoopBlockKeywordLexeme.class, loopBlockKeywordLexeme -> new LoopBlockKeywordToken(loopBlockKeywordLexeme, sourcePosition));
    }

}
