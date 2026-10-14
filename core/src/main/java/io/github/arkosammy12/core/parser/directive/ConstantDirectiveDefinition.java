package io.github.arkosammy12.core.parser.directive;

import io.github.arkosammy12.core.token.FloatLiteralToken;

public sealed interface ConstantDirectiveDefinition permits CalcDefinition, ConstDefinition, LabelDefinition, NextDefinition {

    int getInt32Value();

    int getUInt8Value();

    double getDoubleValue();

    default boolean isUnsigned4Bits() {
        int value = this.getInt32Value();
        return value >= 0 && value <= 15;
    }

    default boolean is8Bits() {
        int value = this.getInt32Value();
        return value >= -128 && value <= 255;
    }

    default boolean isUnsigned12Bits() {
        return FloatLiteralToken.isUnsigned12Bits(this.getInt32Value());
    }

     default boolean isUnsigned16Bits() {
        return FloatLiteralToken.isUnsigned16Bits(this.getInt32Value());
    }

}
