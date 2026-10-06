package io.github.arkosammy12.core.elements;

public sealed abstract class SkipInstructionElement extends InstructionElement permits
        SkipIfKeyNotPressed,
        SkipIfKeyPressed,
        SkipIfRegisterEqualsConstant,
        SkipIfRegistersEqual,
        SkipIfRegistersNotEqual,
        SkipIfRegisterNotEqualsConstant {

    protected final int x;

    public SkipInstructionElement(int offset, int x) {
        super(offset);
        this.x = x;
    }

    abstract public SkipInstructionElement invertCondition();

}
