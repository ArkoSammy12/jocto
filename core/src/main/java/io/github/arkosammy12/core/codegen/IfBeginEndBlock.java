package io.github.arkosammy12.core.codegen;

import java.util.Collection;
import java.util.List;

public final class IfBeginEndBlock extends IfBlock {

    private final Collection<CodeElement> ifBlockStatements;
    private final JumpStatement jumpAboveIfBlockStatement;

    public IfBeginEndBlock(int offset, Collection<CodePrimitive> conditionalExpressionOpcodes, Collection<CodeElement> ifBlockStatements, JumpStatement jumpAboveIfBlockStatement) {
        super(offset, conditionalExpressionOpcodes);
        this.ifBlockStatements = List.copyOf(ifBlockStatements);
        this.jumpAboveIfBlockStatement = jumpAboveIfBlockStatement;
    }

    @Override
    public Collection<CodePrimitive> expand() {
        return List.of();
    }
}
