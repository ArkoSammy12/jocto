package io.github.arkosammy12.core.elements;

import io.github.arkosammy12.core.parser.AddressArgument;

public sealed interface BytesResult {

    byte[] getBytes();

    record Data(byte[] bytes) implements BytesResult {

        @Override
        public byte[] getBytes() {
            return this.bytes();
        }

    }

    record UnresolvedLabel(AddressArgument.Unresolved unresolved, byte[] resolvedBytes) implements BytesResult {

        @Override
        public byte[] getBytes() {
            return this.resolvedBytes();
        }

    }

}
