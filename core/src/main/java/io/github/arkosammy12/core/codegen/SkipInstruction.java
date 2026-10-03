package io.github.arkosammy12.core.codegen;

public sealed abstract class SkipInstruction extends Instruction permits
        SkipIfKeyNotPressed,
        SkipIfKeyPressed,
        SkipIfRegisterEqualsConstant,
        SkipIfRegistersEqual,
        SkipIfRegistersNotEqual,
        SkipIfRegisterNotEqualsConstant {

    protected final int x;

    public SkipInstruction(int offset, int x) {
        super(offset);
        this.x = x;
    }

    abstract public SkipInstruction invertCondition();

}
