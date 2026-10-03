package io.github.arkosammy12.core.codegen;

import java.util.Collection;
import java.util.List;

public final class IfBeginEndBlock extends IfBlock {

    private final JumpStatement jumpAboveIfBlockStatement;
    private final Collection<CodeElement> ifBlockStatements;

    public IfBeginEndBlock(int offset, Collection<CodePrimitive> conditionalExpressionOpcodes, JumpStatement jumpAboveIfBlockStatement, Collection<CodeElement> ifBlockStatements) {
        super(offset, conditionalExpressionOpcodes);
        this.ifBlockStatements = List.copyOf(ifBlockStatements);
        this.jumpAboveIfBlockStatement = jumpAboveIfBlockStatement;
    }

    @Override
    public Collection<CodePrimitive> expand() {
        return List.of();
    }
}
