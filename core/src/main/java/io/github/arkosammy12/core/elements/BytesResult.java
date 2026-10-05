package io.github.arkosammy12.core.elements;

import io.github.arkosammy12.core.parser.AddressArgument;

public sealed interface BytesResult {

    record Data(byte[] bytes) implements BytesResult {}

    record UnresolvedLabel(AddressArgument.Unresolved unresolved) implements BytesResult {}

}
