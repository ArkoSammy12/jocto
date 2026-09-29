package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public non-sealed class IdentifierToken extends NameToken {

    public IdentifierToken(String lexeme, SourcePosition sourcePosition) {
        super(lexeme, sourcePosition);
    }

    @Override
    public String toString() {
        return "IdentifierToken[%s]".formatted(this.getLexemeAndPositionString());
    }

    public static Optional<? extends NameToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        Optional<CalcConstantToken> calcConstantToken = CalcConstantToken.tryParser(lexeme, sourcePosition);
        if (calcConstantToken.isPresent()) {
            return calcConstantToken;
        }
        Optional<CalcOnlyOperatorToken> calcOnlyOperatorToken = CalcOnlyOperatorToken.tryParse(lexeme, sourcePosition);
        if (calcOnlyOperatorToken.isPresent()) {
            return calcOnlyOperatorToken;
        }
        Optional<CallsToken> callsToken = CallsToken.tryParse(lexeme, sourcePosition);
        if (callsToken.isPresent()) {
            return callsToken;
        }
        Optional<DashToken> dashToken = DashToken.tryPase(lexeme, sourcePosition);
        if (dashToken.isPresent()) {
            return dashToken;
        }
        Optional<DelimiterToken> delimiterToken = DelimiterToken.tryParse(lexeme, sourcePosition);
        if (delimiterToken.isPresent()) {
            return delimiterToken;
        }
        return StringModeArgumentToken.tryParse(lexeme, sourcePosition);
    }

}
