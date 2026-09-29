package io.github.arkosammy12.core.lexer;

public interface Lexeme {

    String getLexeme();

    static boolean containsWhitespace(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (Character.isWhitespace(string.charAt(i))) {
                return true;
            }
        }
        return false;
    }

}
