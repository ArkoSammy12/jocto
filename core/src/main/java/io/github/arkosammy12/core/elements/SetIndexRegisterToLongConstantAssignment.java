package io.github.arkosammy12.core.elements;

import io.github.arkosammy12.core.parser.AddressArgument;

public final class SetIndexRegisterToLongConstantAssignment extends Assignment implements LabelableElement {

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
    public BytesResult getBytes() {
        return switch (this.addressArgument) {
            case AddressArgument.Resolved(int nnnn) -> new BytesResult.Data(new byte[] {(byte) 0xF0, (byte) 0x00, (byte)((nnnn >>> 8) & 0xFF), (byte) (nnnn & 0xFF)});
            case AddressArgument.Unresolved unresolved -> new BytesResult.UnresolvedLabel(unresolved);
        };
    }

    @Override
    public AddressArgument getAddressArgument() {
        return this.addressArgument;
    }

    @Override
    public LabelResolveResult resolve(int address) {
        return switch (this.addressArgument) {
            case AddressArgument.Resolved(int resolvedAddress) -> new LabelResolveResult.AlreadyResolved(resolvedAddress);
            case AddressArgument.Unresolved _ -> {
                this.addressArgument = new AddressArgument.Resolved(address);
                yield new LabelResolveResult.Ok();
            }
        };
    }

}
