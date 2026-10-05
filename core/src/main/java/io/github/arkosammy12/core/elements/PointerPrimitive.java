package io.github.arkosammy12.core.elements;

import io.github.arkosammy12.core.parser.AddressArgument;
import io.github.arkosammy12.core.parser.InternalLabelKey;
import io.github.arkosammy12.core.token.Token;

import static io.github.arkosammy12.core.token.IntegerLiteralToken.isUnsigned16Bits;

public final class PointerPrimitive extends CodePrimitive implements LabelableElement {

    private AddressArgument addressArgument;

    public PointerPrimitive(int offset, AddressArgument addressArgument) {
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
            case AddressArgument.Unresolved unresolved -> {
                if (isUnsigned16Bits(address)) {
                    this.addressArgument = new AddressArgument.Resolved(address);
                    yield new LabelResolveResult.Ok();
                } else {
                    yield switch (unresolved) {
                        case AddressArgument.NamedLabelReference(Token token) ->  new LabelResolveResult.LabelResolveError("The label '%s' does not fit in 16 bits!".formatted(token.getLexeme()), unresolved.getSourcePosition());
                        case AddressArgument.InternalLabelReference(InternalLabelKey labelKey) -> new LabelResolveResult.LabelResolveError("The internal label '%s' does not fit in 16 bits!".formatted(labelKey), unresolved.getSourcePosition());
                    };
                }
            }
        };
    }

    @Override
    public String toString() {
        return "PointerPrimitive[%s, address=%s]".formatted(this.getStringBaseContents(), this.addressArgument);
    }

}
