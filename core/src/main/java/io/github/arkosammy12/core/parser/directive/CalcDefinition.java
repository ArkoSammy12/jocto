package io.github.arkosammy12.core.parser.directive;

import static io.github.arkosammy12.core.token.FloatLiteralToken.toInt32JS;
import static io.github.arkosammy12.core.token.FloatLiteralToken.toUInt8JS;

public final class CalcDefinition extends DirectiveDefinition implements ConstantDirectiveDefinition {

    private final double value;

    public CalcDefinition(String name, double value) {
        super(name);
        this.value = value;
    }

    public double getValue() {
        return this.value;
    }

    @Override
    public int getInt32Value() {
        return toInt32JS(this.value);
    }

    @Override
    public int getUInt8Value() {
        return toUInt8JS(this.value);
    }

    @Override
    public double getDoubleValue() {
        return this.value;
    }

}
