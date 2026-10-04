package io.github.arkosammy12.core.parser;

public sealed interface AddressArgument {

    record Resolved(int address) implements AddressArgument {}

    sealed interface Unresolved extends AddressArgument {}

    record NamedLabelReference(String name) implements Unresolved {}

    record InternalLabelReference(InternalLabelKey internalLabelKey) implements Unresolved {}

}
