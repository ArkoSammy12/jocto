package io.github.arkosammy12.core.codegen;

import io.github.arkosammy12.core.parser.AddressArgument;

public final class SetIndexRegisterToLongConstantAssignment extends Assignment {

    private final AddressArgument addressArgument;

    public SetIndexRegisterToLongConstantAssignment(int offset, AddressArgument addressArgument) {
        super(offset);
        this.addressArgument = addressArgument;
    }

    @Override
    int getSizeInBytes() {
        return 4;
    }

}
