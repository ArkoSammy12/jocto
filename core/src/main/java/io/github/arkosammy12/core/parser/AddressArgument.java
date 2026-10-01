package io.github.arkosammy12.core.parser;

public sealed interface AddressArgument {

    record LabelReference(String name) implements AddressArgument {}

    record Value(int value) implements AddressArgument {}

}
