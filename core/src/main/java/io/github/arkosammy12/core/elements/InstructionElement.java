package io.github.arkosammy12.core.elements;

public sealed abstract class InstructionElement extends CodeElement permits Assignment, SkipInstructionElement, Statement {

    public InstructionElement(int offset) {
        super(offset);
    }

}
