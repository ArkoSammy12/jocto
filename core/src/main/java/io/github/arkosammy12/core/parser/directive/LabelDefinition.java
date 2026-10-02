package io.github.arkosammy12.core.parser.directive;

import io.github.arkosammy12.core.token.Token;

import java.util.List;

public final class LabelDefinition extends DirectiveDefinition {

    private final int address;

    public LabelDefinition(String name, int address) {
        super(name);
        this.address = address;
    }

    public int getAddress() {
        return this.address;
    }

    @Override
    public List<Token> expand(int currentOffset) {
        return List.of();
    }

}
