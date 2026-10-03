package io.github.arkosammy12.core.codegen;

import io.github.arkosammy12.core.parser.AddressArgument;

public final class JumpStatement extends Statement implements LabelableInstruction {

    private final AddressArgument addressArgument;

    public JumpStatement(int offset, AddressArgument addressArgument) {
        super(offset);
        this.addressArgument = addressArgument;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public LabelableInstruction resolve(int address) {
        return new JumpStatement(this.offset, new AddressArgument.Value(address));
    }

}
