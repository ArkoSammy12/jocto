package io.github.arkosammy12.core.elements;

import io.github.arkosammy12.core.lexer.SourcePosition;

public sealed interface LabelResolveResult {

    record Ok() implements LabelResolveResult {}

    record AlreadyResolved(int address) implements LabelResolveResult {}

    record LabelResolveError(String error, SourcePosition sourcePosition) implements LabelResolveResult {}

}
