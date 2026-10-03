package io.github.arkosammy12.core.codegen;

import io.github.arkosammy12.core.parser.AddressArgument;

public final class SetIndexRegisterToConstantAssignment extends Assignment implements LabelableInstruction {

    private final AddressArgument addressArgument;

    public SetIndexRegisterToConstantAssignment(int offset, AddressArgument addressArgument) {
        super(offset);
        this.addressArgument = addressArgument;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public LabelableInstruction resolve(int address) {
        return new SetIndexRegisterToConstantAssignment(this.offset, new AddressArgument.Value(address));
    }

}
