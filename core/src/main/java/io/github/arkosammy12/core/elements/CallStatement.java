package io.github.arkosammy12.core.elements;

import io.github.arkosammy12.core.parser.AddressArgument;
import io.github.arkosammy12.core.parser.InternalLabelKey;
import io.github.arkosammy12.core.token.Token;

import static io.github.arkosammy12.core.token.IntegerLiteralToken.isUnsigned12Bits;

public final class CallStatement extends Statement implements LabelableElement {

    private AddressArgument addressArgument;

    public CallStatement(int offset, AddressArgument addressArgument) {
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
            case AddressArgument.Resolved(int nnn) -> new BytesResult.Data(fromNNN(0x2, nnn));
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
                if (isUnsigned12Bits(address)) {
                    this.addressArgument = new AddressArgument.Resolved(address);
                    yield new LabelResolveResult.Ok();
                } else {
                    yield switch (unresolved) {
                        case AddressArgument.NamedLabelReference(Token token) ->  new LabelResolveResult.LabelResolveError("The label '%s' does not fit in 12 bits!".formatted(token.getLexeme()), unresolved.getSourcePosition());
                        case AddressArgument.InternalLabelReference(InternalLabelKey labelKey) -> new LabelResolveResult.LabelResolveError("The internal label '%s' does not fit in 12 bits!".formatted(labelKey), unresolved.getSourcePosition());
                    };
                }
            }
        };
    }

    @Override
    public String toString() {
        return "CallStatement[%s, address=%s]".formatted(this.getStringBaseContents(), this.addressArgument);
    }

}
