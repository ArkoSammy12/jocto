package io.github.arkosammy12.core.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class IfElseBlock extends IfBlock {

    private final JumpStatement jumpAboveIfBlockStatement;
    private final Collection<CodeElement> ifBlockStatements;
    private final JumpStatement jumpAboveElseBlockStatement;
    private final Collection<CodeElement> elseBlockStatements;

    public IfElseBlock(Collection<CodePrimitive> conditionalExpressionOpcodes, JumpStatement jumpAboveIfBlockStatement, Collection<CodeElement> ifBlockStatements, JumpStatement jumpAboveElseBlockStatement, Collection<CodeElement> elseBlockStatements) {
        super(conditionalExpressionOpcodes);
        this.ifBlockStatements = List.copyOf(ifBlockStatements);
        this.jumpAboveIfBlockStatement = jumpAboveIfBlockStatement;
        this.elseBlockStatements = List.copyOf(elseBlockStatements);
        this.jumpAboveElseBlockStatement = jumpAboveElseBlockStatement;
    }

    @Override
    public Collection<CodePrimitive> expand() {
        List<CodePrimitive> codePrimitives = new ArrayList<>(this.conditionalExpressionOpcodes);
        codePrimitives.add(this.jumpAboveIfBlockStatement);
        for (CodeElement ifBlockStatements : this.ifBlockStatements) {
            switch (ifBlockStatements) {
                case CodePrimitive codePrimitive -> codePrimitives.add(codePrimitive);
                case CodeBlock codeBlock -> codePrimitives.addAll(codeBlock.expand());
            }
        }
        codePrimitives.add(this.jumpAboveElseBlockStatement);
        for (CodeElement elseBlockStatements : this.elseBlockStatements) {
            switch (elseBlockStatements) {
                case CodePrimitive codePrimitive -> codePrimitives.add(codePrimitive);
                case CodeBlock codeBlock -> codePrimitives.addAll(codeBlock.expand());
            }
        }
        return List.copyOf(codePrimitives);
    }

}
