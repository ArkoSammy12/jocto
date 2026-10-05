package io.github.arkosammy12.core.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class IfBeginEndBlock extends IfBlock {

    private final JumpStatement jumpAboveIfBlockStatement;
    private final Collection<CodeElement> ifBlockStatements;

    public IfBeginEndBlock(Collection<CodePrimitive> conditionalExpressionOpcodes, JumpStatement jumpAboveIfBlockStatement, Collection<CodeElement> ifBlockStatements) {
        super(conditionalExpressionOpcodes);
        this.ifBlockStatements = List.copyOf(ifBlockStatements);
        this.jumpAboveIfBlockStatement = jumpAboveIfBlockStatement;
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
        return List.copyOf(codePrimitives);
    }

    @Override
    public String toString() {
        return "IfBeginBlock[%s]".formatted(this.getStringBaseContents());
    }

}
