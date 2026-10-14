package io.github.arkosammy12.core.parser.directive;

public final class ConstDefinition extends DirectiveDefinition implements ConstantDirectiveDefinition {

    private final int value;

    public ConstDefinition(String name, int value) {
        super(name);
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    @Override
    public int getInt32Value() {
        return this.value;
    }

    @Override
    public int getUInt8Value() {
        return this.value;
    }

    @Override
    public double getDoubleValue() {
        return this.value;
    }

}