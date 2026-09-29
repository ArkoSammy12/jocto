package io.github.arkosammy12.core.token;

import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

import static io.github.arkosammy12.core.lexer.CharacterSet.HEXADECIMAL_DIGITS;

public final class RegisterLiteralToken extends LiteralToken {

    private final int registerIndex;

    public RegisterLiteralToken(int registerIndex, String lexeme, SourcePosition sourcePosition) {
        if (registerIndex < 0 || registerIndex > 15) {
            throw new IllegalArgumentException("Cannot create register token with an index of '%d'".formatted(registerIndex));
        }
        super(lexeme, sourcePosition);
        this.registerIndex = registerIndex;
    }

    public int getRegisterIndex() {
        return this.registerIndex;
    }

    @Override
    public String toString() {
        return "RegisterLiteralIndex[%s, registerIndex=%d]".formatted(this.getLexemeAndPositionString(), this.registerIndex);
    }

    public static Optional<RegisterLiteralToken> tryParse(String lexeme, SourcePosition sourcePosition) {
        if (lexeme.length() != 2) {
            return Optional.empty();
        }
        if (lexeme.charAt(0) != 'v') {
            return Optional.empty();
        }
        if (!HEXADECIMAL_DIGITS.contains(lexeme.charAt(1))) {
            return Optional.empty();
        }
        return Optional.of(new RegisterLiteralToken(Integer.parseInt(String.valueOf(lexeme.charAt(1)), 16), lexeme, sourcePosition));
    }

}
