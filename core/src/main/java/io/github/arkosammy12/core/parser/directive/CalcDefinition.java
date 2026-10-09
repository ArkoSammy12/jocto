package io.github.arkosammy12.core.parser.directive;

public final class CalcDefinition extends DirectiveDefinition {

    private final double value;

    public CalcDefinition(String name, double value) {
        super(name);
        this.value = value;
    }

    public double getValue() {
        return this.value;
    }

}
