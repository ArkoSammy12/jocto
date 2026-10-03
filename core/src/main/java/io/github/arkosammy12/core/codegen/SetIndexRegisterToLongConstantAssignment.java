package io.github.arkosammy12.core.codegen;

import io.github.arkosammy12.core.parser.AddressArgument;

public final class SetIndexRegisterToLongConstantAssignment extends Assignment implements LabelableInstruction {

    private AddressArgument addressArgument;

    public SetIndexRegisterToLongConstantAssignment(int offset, AddressArgument addressArgument) {
        super(offset);
        this.addressArgument = addressArgument;
    }

    @Override
    public int getSizeInBytes() {
        return 4;
    }

    @Override
    public void resolve(int address) {
        if (!(addressArgument instanceof AddressArgument.Resolved)) {
            this.addressArgument = new AddressArgument.Resolved(address);
        }
    }

}
