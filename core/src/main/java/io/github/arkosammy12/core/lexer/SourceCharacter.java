package io.github.arkosammy12.core.lexer;

import java.util.Collection;

public record SourceCharacter(char character, SourcePosition sourcePosition) {

    public static String joinAsString(Collection<SourceCharacter> sourceCharacters) {
        return sourceCharacters.stream().map(sourceCharacter -> String.valueOf(sourceCharacter.character())).reduce((a, b) -> a + b).orElse("");
    }

}
