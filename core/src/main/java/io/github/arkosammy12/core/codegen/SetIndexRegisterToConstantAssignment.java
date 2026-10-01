package io.github.arkosammy12.core.codegen;

import io.github.arkosammy12.core.parser.AddressArgument;

public final class SetIndexRegisterToConstantAssignment extends Assignment {

    private final AddressArgument addressArgument;

    public SetIndexRegisterToConstantAssignment(int offset, AddressArgument addressArgument) {
        super(offset);
        this.addressArgument = addressArgument;
    }

    @Override
    int getSizeInBytes() {
        return 2;
    }

}
