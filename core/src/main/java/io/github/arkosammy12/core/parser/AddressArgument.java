package io.github.arkosammy12.core.parser;

import io.github.arkosammy12.core.lexer.SourcePosition;

public sealed interface AddressArgument {

    record LabelReference(String name) implements AddressArgument {}

    record Value(int value) implements AddressArgument {}

    sealed interface InternalPlaceholder extends AddressArgument {

        SourcePosition getSourcePosition();

    }

    record WhileForwardJumpPlaceholder(SourcePosition sourcePosition) implements InternalPlaceholder {

        @Override
        public SourcePosition getSourcePosition() {
            return this.sourcePosition();
        }

    }

    record AgainForwardJumpPlaceholder(SourcePosition sourcePosition) implements InternalPlaceholder {

        @Override
        public SourcePosition getSourcePosition() {
            return this.sourcePosition();
        }

    }

}
