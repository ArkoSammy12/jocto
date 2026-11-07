package io.github.arkosammy12.jocto.core.elements;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;

public sealed interface LabelResolveResult {

    record Ok() implements LabelResolveResult {}

    record AlreadyResolved(int address) implements LabelResolveResult {}

    record LabelResolveError(String error, SourcePosition sourcePosition) implements LabelResolveResult {}

}
