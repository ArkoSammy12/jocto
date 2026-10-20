package io.github.arkosammy12.core.lexer;

import static io.github.arkosammy12.core.lexer.OctoLexer.isOctoWhitespace;

public interface Lexeme {

    String getLexeme();

    static boolean containsWhitespace(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (isOctoWhitespace(string.charAt(i))) {
                return true;
            }
        }
        return false;
    }

}
