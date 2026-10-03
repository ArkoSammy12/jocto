package io.github.arkosammy12.core.codegen;

import java.util.Collection;
import java.util.List;

public abstract sealed class IfBlock extends CodeBlock permits IfBeginEndBlock, IfElseBlock, IfThenBlock {

    protected final Collection<CodePrimitive> conditionalExpressionOpcodes;

    public IfBlock(int offset, Collection<CodePrimitive> conditionalExpressionOpcodes) {
        super(offset);
        this.conditionalExpressionOpcodes = List.copyOf(conditionalExpressionOpcodes);
    }

}
