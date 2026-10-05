package io.github.arkosammy12.core.parser.directive;

public final class LabelDefinition extends DirectiveDefinition {

    private final int address;

    public LabelDefinition(String name, int address) {
        super(name);
        this.address = address;
    }

    public int getAddress() {
        return this.address;
    }

}
