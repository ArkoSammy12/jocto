package io.github.arkosammy12.core.parser.directive;

public final class LabelDefinition extends DirectiveDefinition implements ConstantDirectiveDefinition {

    private final int address;

    public LabelDefinition(String name, int address) {
        super(name);
        this.address = address;
    }

    public int getAddress() {
        return this.address;
    }

    @Override
    public int getInt32Value() {
        return this.address;
    }

    @Override
    public int getUInt8Value() {
        return this.address;
    }

    @Override
    public double getDoubleValue() {
        return this.address;
    }

}
