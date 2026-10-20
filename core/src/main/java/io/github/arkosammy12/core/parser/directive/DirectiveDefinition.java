package io.github.arkosammy12.core.parser.directive;

public sealed abstract class DirectiveDefinition permits ConstantDirectiveDefinition, NonConstantDirectiveDefinition {

    protected final String name;

    public DirectiveDefinition(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

}
