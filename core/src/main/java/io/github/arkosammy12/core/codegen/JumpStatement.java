package io.github.arkosammy12.core.codegen;

import io.github.arkosammy12.core.parser.AddressArgument;

public final class JumpStatement extends Statement {

    private final AddressArgument addressArgument;

    public JumpStatement(int offset, AddressArgument addressArgument) {
        super(offset);
        this.addressArgument = addressArgument;
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
