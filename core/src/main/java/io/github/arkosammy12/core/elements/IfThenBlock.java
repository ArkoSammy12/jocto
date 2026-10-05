package io.github.arkosammy12.core.elements;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class IfThenBlock extends IfBlock {

    @Nullable
    private final CodeElement codeElement;

    public IfThenBlock(Collection<CodePrimitive> conditionalExpressionOpcodes, @Nullable CodeElement codeElement) {
        super(conditionalExpressionOpcodes);
        this.codeElement = codeElement;
    }

    public IfThenBlock(Collection<CodePrimitive> conditionalExpressionOpcodes) {
        this(conditionalExpressionOpcodes, null);
    }

    @Override
    public Collection<CodePrimitive> expand() {
        List<CodePrimitive> codePrimitives = new ArrayList<>(this.conditionalExpressionOpcodes);
        switch (this.codeElement) {
            case CodePrimitive codePrimitive -> codePrimitives.add(codePrimitive);
            case CodeBlock codeBlock -> codePrimitives.addAll(codeBlock.expand());
            case null -> {}
        }
        return List.copyOf(codePrimitives);
    }

    @Override
    public String toString() {
        return "IfThenBlock[%s]".formatted(this.getStringBaseContents());
    }

}
