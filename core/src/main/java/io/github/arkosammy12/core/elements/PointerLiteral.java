package io.github.arkosammy12.core.elements;

import io.github.arkosammy12.core.parser.AddressArgument;

public final class PointerLiteral extends CodePrimitive implements LabelableElement {

    private AddressArgument addressArgument;

    public PointerLiteral(int offset, AddressArgument addressArgument) {
        super(offset);
        this.addressArgument = addressArgument;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public BytesResult getBytes() {
        return switch (this.addressArgument) {
            case AddressArgument.Resolved(int address) -> new BytesResult.Data(new byte[] {(byte) 0xF0, (byte) 0x00, (byte)((address >>> 8) & 0xFF), (byte) (address & 0xFF)});
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
