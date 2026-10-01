package io.github.arkosammy12.core.parser.directive;

import io.github.arkosammy12.core.token.Token;

import java.util.List;

public sealed abstract class DirectiveDefinition permits AliasDefinition, LabelDefinition, MacroDefinition {

    private final String name;

    public DirectiveDefinition(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    abstract public List<Token> expand(int currentOffset);

}
