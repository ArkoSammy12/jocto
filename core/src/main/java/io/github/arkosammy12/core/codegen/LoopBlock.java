package io.github.arkosammy12.core.codegen;

import java.util.Collection;
import java.util.List;

public final class LoopBlock extends CodeBlock {

    public LoopBlock(int offset) {
        super(offset);
    }

    @Override
    public Collection<CodePrimitive> expand() {
        return List.of();
    }

}
