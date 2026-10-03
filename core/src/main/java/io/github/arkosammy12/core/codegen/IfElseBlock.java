package io.github.arkosammy12.core.codegen;

import java.util.Collection;
import java.util.List;

public final class IfElseBlock extends IfBlock {

    private final Collection<CodeElement> ifBlockStatements;
    private final JumpStatement jumpAboveIfBlockStatement;
    private final Collection<CodeElement> elseBlockStatements;
    private final JumpStatement jumpAboveElseBlockStatement;

    public IfElseBlock(int offset, Collection<CodePrimitive> conditionalExpressionOpcodes, Collection<CodeElement> ifBlockStatements, JumpStatement jumpAboveIfBlockStatement, Collection<CodeElement> elseBlockStatements, JumpStatement jumpAboveElseBlockStatement) {
        super(offset, conditionalExpressionOpcodes);
        this.ifBlockStatements = List.copyOf(ifBlockStatements);
        this.jumpAboveIfBlockStatement = jumpAboveIfBlockStatement;
        this.elseBlockStatements = List.copyOf(elseBlockStatements);
        this.jumpAboveElseBlockStatement = jumpAboveElseBlockStatement;
    }

    @Override
    public Collection<CodePrimitive> expand() {
        return List.of();
    }

}
