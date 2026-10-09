package io.github.arkosammy12.core.parser.directive;

public sealed abstract class DirectiveDefinition permits AliasDefinition, CalcDefinition, ConstDefinition, LabelDefinition, MacroDefinition, NextDefinition {

    protected final String name;

    public DirectiveDefinition(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

}
