package io.github.arkosammy12.jocto.core.token;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.grammar.IfBlockKeywordLexeme;
import io.github.arkosammy12.jocto.core.lexer.SourcePosition;

public final class IfBlockKeywordToken extends ReservedNameToken {

    private final IfBlockKeywordLexeme ifBlockKeywordLexeme;

    public IfBlockKeywordToken(IfBlockKeywordLexeme ifBlockKeywordLexeme, SourcePosition sourcePosition) {
        super(ifBlockKeywordLexeme.getLexeme(), sourcePosition);
        this.ifBlockKeywordLexeme = ifBlockKeywordLexeme;
    }

    public IfBlockKeywordLexeme getIfBlockKeywordLexeme() {
        return this.ifBlockKeywordLexeme;
    }

    @Override
    public String toString() {
        return "IfBlockKeywordToken[%s, keyword=%s]".formatted(this.getBaseStringContents(), this.ifBlockKeywordLexeme.name());
    }

    public static Optional<IfBlockKeywordToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        return Token.findMatching(lexeme, IfBlockKeywordLexeme.class, ifBlockKeywordLexeme -> new IfBlockKeywordToken(ifBlockKeywordLexeme, sourcePosition));
    }

}
