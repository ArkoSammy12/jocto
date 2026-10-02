package io.github.arkosammy12.core.parser.directive;

import io.github.arkosammy12.core.token.Token;

import java.util.List;

public final class AliasDefinition extends DirectiveDefinition {

    private final int registerIndex;

    public AliasDefinition(String name, int registerIndex) {
        super(name);
        this.registerIndex = registerIndex;
    }

    public int getRegisterIndex() {
        return this.registerIndex;
    }

    @Override
    public List<Token> expand(int currentOffset) {
        return List.of();
    }

}
