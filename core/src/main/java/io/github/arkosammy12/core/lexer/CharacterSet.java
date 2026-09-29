package io.github.arkosammy12.core.lexer;

import java.util.Objects;

public final class CharacterSet {

    public static final CharacterSet BINARY_DIGITS = new CharacterSet("01");

    public static final CharacterSet DECIMAL_DIGITS = BINARY_DIGITS.append("23456789");

    public static final CharacterSet HEXADECIMAL_DIGITS = DECIMAL_DIGITS.append("abcdefABCDEF");

    private final String chars;

    private CharacterSet(String chars) {
        Objects.requireNonNull(chars);
        this.chars = chars;
    }

    public boolean contains(String str) {
        return str.chars().allMatch(c -> this.chars.contains(String.valueOf(c)));
    }

    public boolean contains(char c) {
        return this.chars.indexOf(c) >= 0;
    }

    public CharacterSet append(String other) {
        return new CharacterSet(this.chars + other);
    }

}
