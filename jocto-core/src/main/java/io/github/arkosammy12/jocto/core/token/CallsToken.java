package io.github.arkosammy12.jocto.core.token;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;

public final class CallsToken extends IdentifierToken {

    public CallsToken(SourcePosition sourcePosition) {
        super("CALLS", sourcePosition);
    }

    @Override
    public String toString() {
        return "MacroCALLSIdentifierToken[%s]".formatted(this.getBaseStringContents());
    }

    public static Optional<CallsToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        if ("CALLS".equals(lexeme)) {
            return Optional.of(new CallsToken(sourcePosition));
        } else {
            return Optional.empty();
        }
    }

}
