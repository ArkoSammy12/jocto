package io.github.arkosammy12.core.parser.directive;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.token.IntegerLiteralToken;
import io.github.arkosammy12.core.token.Token;

import java.util.List;

public final class ConstDefinition extends DirectiveDefinition implements ExpandableDirective {

    private final int value;

    public ConstDefinition(String name, int value) {
        super(name);
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    @Override
    public List<Token> expand(int currentOffset, SourcePosition sourcePosition) {
        return List.of(new IntegerLiteralToken(this.value, this.name, sourcePosition));
    }

}