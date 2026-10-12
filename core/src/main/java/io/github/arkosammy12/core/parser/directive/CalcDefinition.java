package io.github.arkosammy12.core.parser.directive;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.token.FloatLiteralToken;
import io.github.arkosammy12.core.token.Token;

import java.util.List;

public final class CalcDefinition extends DirectiveDefinition implements ExpandableDirective {

    private final double value;

    public CalcDefinition(String name, double value) {
        super(name);
        this.value = value;
    }

    public double getValue() {
        return this.value;
    }

    @Override
    public List<Token> expand(int currentOffset, SourcePosition sourcePosition) {
        return List.of(new FloatLiteralToken(this.value, this.name, sourcePosition));
    }
}
