package io.github.arkosammy12.core.codegen;

public sealed abstract class Instruction extends CodePrimitive permits Assignment, SkipInstruction, Statement {

    public Instruction(int offset) {
        super(offset);
    }

}
