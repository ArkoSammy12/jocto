package io.github.arkosammy12.core.elements;

import io.github.arkosammy12.core.parser.AddressArgument;
import io.github.arkosammy12.core.parser.InternalLabelKey;
import io.github.arkosammy12.core.token.Token;

import static io.github.arkosammy12.core.token.NumberLiteralToken.isUnsigned12Bits;
import static io.github.arkosammy12.core.token.NumberLiteralToken.isUnsigned16Bits;

public final class UnpackElement extends CodeElement implements LabelableElement {

    private final int highRegisterIndex;
    private final int lowRegisterIndex;
    private final int highNibble;
    private AddressArgument addressArgument;

    public UnpackElement(int offset, int highRegisterIndex, int lowRegisterIndex, int highNibble, AddressArgument addressArgument) {
        if (highNibble > 0xF) {
            throw new IllegalArgumentException("High nibble of unpack element cannot be greater than 15!");
        }
        super(offset);
        this.highRegisterIndex = highRegisterIndex;
        this.lowRegisterIndex = lowRegisterIndex;
        this.highNibble = highNibble;
        this.addressArgument = addressArgument;
    }

    public UnpackElement(int offset, int highRegisterIndex, int lowRegisterIndex, AddressArgument addressArgument) {
        this(offset, highRegisterIndex, lowRegisterIndex, -1, addressArgument);
    }

    @Override
    public int getSizeInBytes() {
        return 4;
    }

    @Override
    public BytesResult getBytes() {
        if (this.highNibble >= 0) {
            return switch (this.addressArgument) {
                case AddressArgument.Resolved(int address) -> new BytesResult.Data(new byte[] {(byte) (0x60 | (this.highRegisterIndex & 0xF)), (byte) (((address >>> 8) & 0xF) | ((this.highNibble & 0xF) << 4)), (byte) (0x60 | (this.lowRegisterIndex & 0xF)), (byte) (address & 0xFF)});
                case AddressArgument.Unresolved unresolved -> new BytesResult.UnresolvedLabel(unresolved, new byte[] {(byte) (0x60 | (this.highRegisterIndex & 0xF)), (byte) ((this.highNibble & 0xF) << 4), (byte) (0x60 | (this.lowRegisterIndex & 0xF)), (byte) 0x00});
            };
        } else {
            return switch (this.addressArgument) {
                case AddressArgument.Resolved(int address) -> new BytesResult.Data(new byte[] {(byte) (0x60 | (this.highRegisterIndex & 0xF)), (byte) ((address >>> 8) & 0xFF), (byte) (0x60 | (this.lowRegisterIndex & 0xF)), (byte) (address & 0xFF)});
                case AddressArgument.Unresolved unresolved -> new BytesResult.UnresolvedLabel(unresolved, new byte[] {(byte) (0x60 | (this.highRegisterIndex & 0xF)), (byte) 0x00, (byte) (0x60 | (this.lowRegisterIndex & 0xF)), (byte) 0x00});
            };
        }
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
                if (this.highNibble >= 0) {
                    if (isUnsigned12Bits(address)) {
                        this.addressArgument = new AddressArgument.Resolved(address);
                        yield new LabelResolveResult.Ok();
                    } else {
                        yield switch (unresolved) {
                            case AddressArgument.NamedLabelReference(Token token) ->  new LabelResolveResult.LabelResolveError("The label '%s' does not fit in 12 bits!".formatted(token.getLexeme()), unresolved.getSourcePosition());
                            case AddressArgument.InternalLabelReference(InternalLabelKey labelKey) -> new LabelResolveResult.LabelResolveError("The internal label '%s' does not fit in 12 bits!".formatted(labelKey), unresolved.getSourcePosition());
                        };
                    }
                } else {
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
            }
        };
    }

}
