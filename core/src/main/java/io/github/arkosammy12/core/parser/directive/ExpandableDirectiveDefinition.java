package io.github.arkosammy12.core.parser.directive;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.result.OctoAssemblerException;
import io.github.arkosammy12.core.token.Token;

import java.util.List;

public abstract sealed class ExpandableDirectiveDefinition extends NonConstantDirectiveDefinition permits MacroDefinition, StringModeDefinition {

    public ExpandableDirectiveDefinition(String name) {
        super(name);
    }

    abstract public int getExpansionArgumentQuantity();

    abstract public List<Token> expand(int currentOffset, SourcePosition sourcePosition, Token... expansionArguments) throws OctoAssemblerException;

}
