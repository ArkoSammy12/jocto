package io.github.arkosammy12.core.parser;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.token.Token;

public sealed interface AddressArgument {

    record Resolved(int address) implements AddressArgument {}

    sealed interface Unresolved extends AddressArgument {

        SourcePosition getSourcePosition();

    }

    record NamedLabelReference(Token token) implements Unresolved {

        @Override
        public SourcePosition getSourcePosition() {
            return this.token().getSourcePosition();
        }

    }

    record InternalLabelReference(InternalLabelKey internalLabelKey) implements Unresolved {

        @Override
        public SourcePosition getSourcePosition() {
            return this.internalLabelKey().sourcePositionKey();
        }

    }

}
