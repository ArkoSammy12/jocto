package io.github.arkosammy12.core.parser.directive;

import io.github.arkosammy12.core.token.FloatLiteralToken;

public abstract sealed class ConstantDirectiveDefinition extends DirectiveDefinition permits ConstDefinition, CalcDefinition, LabelDefinition, NextDefinition {

    public ConstantDirectiveDefinition(String name) {
        super(name);
    }

    abstract public int getInt32Value();

    abstract public int getUInt8Value();

    abstract public double getDoubleValue();

    public boolean isUnsigned4Bits() {
        int value = this.getInt32Value();
        return value >= 0 && value <= 15;
    }

    public boolean is8Bits() {
        int value = this.getInt32Value();
        return value >= -128 && value <= 255;
    }

    public boolean isUnsigned12Bits() {
        return FloatLiteralToken.isUnsigned12Bits(this.getInt32Value());
    }

    public boolean isUnsigned16Bits() {
        return FloatLiteralToken.isUnsigned16Bits(this.getInt32Value());
    }

}
