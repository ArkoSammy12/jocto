package io.github.arkosammy12.core.parser.directive;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.lexer.SourceStream;
import io.github.arkosammy12.core.token.Token;

import java.util.Collection;
import java.util.List;

public final class MacroDefinition extends DirectiveDefinition implements ExpandableDirective {

    private final Collection<String> arguments;
    private final SourceStream<Token> tokens;

    public MacroDefinition(String name, Collection<String> arguments, SourceStream<Token> token) {
        super(name);
        this.arguments = List.copyOf(arguments);
        this.tokens = new SourceStream<>(token);
    }

    public Collection<String> getArguments() {
        return this.arguments;
    }

    public SourceStream<Token> getTokens() {
        return this.tokens;
    }

    @Override
    public List<Token> expand(int currentOffset, SourcePosition sourcePosition) {
        return List.of();
    }

}
