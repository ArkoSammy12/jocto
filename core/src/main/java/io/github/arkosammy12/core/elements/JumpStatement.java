package io.github.arkosammy12.core.elements;

import io.github.arkosammy12.core.parser.AddressArgument;

public final class JumpStatement extends Statement implements LabelableElement {

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
    public BytesResult getBytes() {
        return switch (this.addressArgument) {
            case AddressArgument.Resolved(int nnn) -> new BytesResult.Data(fromNNN(0x1, nnn));
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
