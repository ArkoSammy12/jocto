package io.github.arkosammy12.core.codegen;

import io.github.arkosammy12.core.parser.AddressArgument;

public final class JumpZeroStatement extends Statement {

    private final AddressArgument addressArgument;

    public JumpZeroStatement(int offset, AddressArgument addressArgument) {
        super(offset);
        this.addressArgument = addressArgument;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

}
