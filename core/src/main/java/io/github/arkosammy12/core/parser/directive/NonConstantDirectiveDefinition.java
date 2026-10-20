package io.github.arkosammy12.core.parser.directive;

public abstract sealed class NonConstantDirectiveDefinition extends DirectiveDefinition permits AliasDefinition, ExpandableDirectiveDefinition {

    public NonConstantDirectiveDefinition(String name) {
        super(name);
    }

}
