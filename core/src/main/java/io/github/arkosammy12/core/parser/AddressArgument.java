package io.github.arkosammy12.core.parser;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.token.Token;
import org.jetbrains.annotations.NotNull;

public sealed interface AddressArgument {

    record Resolved(int address) implements AddressArgument {

        @Override
        @NotNull
        public String toString() {
            return "AddressArgument.Resolved[address=0x%04X]".formatted(this.address);
        }

    }

    sealed interface Unresolved extends AddressArgument {

        SourcePosition getSourcePosition();

    }

    record NamedLabelReference(Token token) implements Unresolved {

        @Override
        public SourcePosition getSourcePosition() {
            return this.token().getSourcePosition();
        }

        @Override
        @NotNull
        public String toString() {
            return "AddressArgument.NamedLabelReference[token=%s]".formatted(this.token);
        }

    }

    record InternalLabelReference(InternalLabelKey internalLabelKey) implements Unresolved {

        @Override
        public SourcePosition getSourcePosition() {
            return this.internalLabelKey().sourcePositionKey();
        }

        @Override
        @NotNull
        public String toString() {
            return "AddressArgument.InternalLabelReference[internalLabelKey=%s]".formatted(this.internalLabelKey);
        }

    }

}
