package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;

public abstract sealed class NumberLiteralToken extends LiteralToken permits FloatLiteralToken, IntegerLiteralToken {

    public NumberLiteralToken(String lexeme, SourcePosition sourcePosition) {
        super(lexeme, sourcePosition);
    }

}
