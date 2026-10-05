package io.github.arkosammy12.core.elements;

import io.github.arkosammy12.core.parser.AddressArgument;

public interface LabelableElement {

    AddressArgument getAddressArgument();

    LabelResolveResult resolve(int address);

}
