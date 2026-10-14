package io.github.arkosammy12.core.parser.directive;

public final class AliasDefinition extends DirectiveDefinition {

    private final int registerIndex;

    public AliasDefinition(String name, int registerIndex) {
        super(name);
        this.registerIndex = registerIndex;
    }

    public int getRegisterIndex() {
        return this.registerIndex;
    }

}
