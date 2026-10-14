package io.github.arkosammy12.core.parser.directive;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.result.OctoAssemblerException;
import io.github.arkosammy12.core.token.Token;

import java.util.List;

public sealed interface ExpandableDirective permits MacroDefinition {

    int getExpansionArgumentQuantity();

    List<Token> expand(int currentOffset, SourcePosition sourcePosition, Token... expansionArguments) throws OctoAssemblerException;

}
