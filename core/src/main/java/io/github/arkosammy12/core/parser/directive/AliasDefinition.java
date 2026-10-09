package io.github.arkosammy12.core.parser.directive;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.token.RegisterLiteralToken;
import io.github.arkosammy12.core.token.Token;

import java.util.List;

public final class AliasDefinition extends DirectiveDefinition implements ExpandableDirective {

    private final int registerIndex;

    public AliasDefinition(String name, int registerIndex) {
        super(name);
        this.registerIndex = registerIndex;
    }

    @Override
    public List<Token> expand(int currentOffset, SourcePosition sourcePosition) {
        return List.of(new RegisterLiteralToken(this.registerIndex, this.name, sourcePosition));
    }

}
