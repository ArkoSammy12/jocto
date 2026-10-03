package io.github.arkosammy12.core.codegen;

import java.util.Collection;

public sealed abstract class CodeBlock extends CodeElement permits IfBlock {

    abstract public Collection<CodePrimitive> expand();

}
