package io.github.arkosammy12.jocto.core.parser;

import org.jetbrains.annotations.NotNull;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;

public record InternalLabelKey(long labelId, int addressKey, SourcePosition sourcePositionKey) {

    @Override
    @NotNull
    public String toString() {
        return "InternalLabelKey[labelId=%d, addressKey=$%04X, sourcePositionKey=%s]".formatted(this.labelId, this.addressKey, this.sourcePositionKey);
    }

}
