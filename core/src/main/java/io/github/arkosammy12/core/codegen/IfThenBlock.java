package io.github.arkosammy12.core.codegen;

import org.jetbrains.annotations.Nullable;

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
        return List.of();
    }

}
