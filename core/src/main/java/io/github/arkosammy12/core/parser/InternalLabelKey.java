package io.github.arkosammy12.core.parser;

import io.github.arkosammy12.core.lexer.SourcePosition;
import org.jetbrains.annotations.NotNull;

public record InternalLabelKey(long labelId, int addressKey, SourcePosition sourcePositionKey) {

    @Override
    @NotNull
    public String toString() {
        return "InternalLabelKey[labelId=%d, addressKey=$%04X, sourcePositionKey=%s]".formatted(this.labelId, this.addressKey, this.sourcePositionKey);
    }

}
