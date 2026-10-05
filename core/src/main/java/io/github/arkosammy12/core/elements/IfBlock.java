package io.github.arkosammy12.core.elements;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract sealed class IfBlock extends CodeBlock permits IfBeginEndBlock, IfElseBlock, IfThenBlock {

    protected final Collection<CodePrimitive> conditionalExpressionOpcodes;

    public IfBlock(Collection<CodePrimitive> conditionalExpressionOpcodes) {
        this.conditionalExpressionOpcodes = List.copyOf(conditionalExpressionOpcodes);
    }

    protected static String joinPrimitives(Collection<CodePrimitive> codePrimitives) {
        return codePrimitives.stream().map(CodePrimitive::toString).collect(Collectors.joining(", "));
    }

    protected String getStringBaseContents() {
        return "contents=[%s]".formatted(joinPrimitives(this.expand()));
    }

    @Override
    public String toString() {
        return "IfBlock[%s]".formatted(this.getStringBaseContents());
    }

}
