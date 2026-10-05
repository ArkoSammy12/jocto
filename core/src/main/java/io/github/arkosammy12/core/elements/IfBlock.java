package io.github.arkosammy12.core.elements;

import java.util.Collection;
import java.util.List;

public abstract sealed class IfBlock extends CodeBlock permits IfBeginEndBlock, IfElseBlock, IfThenBlock {

    protected final Collection<CodePrimitive> conditionalExpressionOpcodes;

    public IfBlock(Collection<CodePrimitive> conditionalExpressionOpcodes) {
        this.conditionalExpressionOpcodes = List.copyOf(conditionalExpressionOpcodes);
    }

}
