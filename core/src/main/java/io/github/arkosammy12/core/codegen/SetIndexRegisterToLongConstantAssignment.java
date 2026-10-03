package io.github.arkosammy12.core.codegen;

import io.github.arkosammy12.core.parser.AddressArgument;

public final class SetIndexRegisterToLongConstantAssignment extends Assignment implements LabelableInstruction {

    private final AddressArgument addressArgument;

    public SetIndexRegisterToLongConstantAssignment(int offset, AddressArgument addressArgument) {
        super(offset);
        this.addressArgument = addressArgument;
    }

    @Override
    public int getSizeInBytes() {
        return 4;
    }

    @Override
    public LabelableInstruction resolve(int address) {
        return new SetIndexRegisterToLongConstantAssignment(this.offset, new AddressArgument.Value(address));
    }

}
