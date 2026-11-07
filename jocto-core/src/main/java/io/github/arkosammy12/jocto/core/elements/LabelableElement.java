package io.github.arkosammy12.jocto.core.elements;

import io.github.arkosammy12.jocto.core.parser.AddressArgument;

public interface LabelableElement {

    AddressArgument getAddressArgument();

    LabelResolveResult resolve(int address);

}
