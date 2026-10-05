package io.github.arkosammy12.core.elements;

import java.util.Collection;

public sealed abstract class CodeBlock extends CodeElement permits IfBlock {

    abstract public Collection<CodePrimitive> expand();

}
