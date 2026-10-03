package io.github.arkosammy12.core.codegen;

import io.github.arkosammy12.core.parser.AddressArgument;

public final class JumpStatement extends Statement implements LabelableInstruction {

    private AddressArgument addressArgument;

    public JumpStatement(int offset, AddressArgument addressArgument) {
        super(offset);
        this.addressArgument = addressArgument;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public void resolve(int address) {
        if (!(addressArgument instanceof AddressArgument.Resolved)) {
            this.addressArgument = new AddressArgument.Resolved(address);
        }
    }

}
