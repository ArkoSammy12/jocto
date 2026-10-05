package io.github.arkosammy12.core.parser;

import io.github.arkosammy12.core.lexer.SourcePosition;
import org.jetbrains.annotations.NotNull;

public record InternalLabelKey(int addressKey, SourcePosition sourcePositionKey) {

    @Override
    @NotNull
    public String toString() {
        return "InternalLabelKey[addressKey=%04X, sourcePosition=%s]".formatted(this.addressKey, this.sourcePositionKey);
    }

}
