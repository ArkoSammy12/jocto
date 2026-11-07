package io.github.arkosammy12.jocto.core.parser.directive;

import java.util.List;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;
import io.github.arkosammy12.jocto.core.result.OctoAssemblerException;
import io.github.arkosammy12.jocto.core.token.Token;

public abstract sealed class ExpandableDirectiveDefinition extends NonConstantDirectiveDefinition permits MacroDefinition, StringModeDefinition {

    public ExpandableDirectiveDefinition(String name) {
        super(name);
    }

    abstract public int getExpansionArgumentQuantity();

    abstract public List<Token> expand(int currentOffset, SourcePosition sourcePosition, Token... expansionArguments) throws OctoAssemblerException;

}
