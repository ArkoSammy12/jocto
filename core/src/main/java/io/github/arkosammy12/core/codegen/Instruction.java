package io.github.arkosammy12.core.codegen;

public sealed abstract class Instruction extends CodeElement permits Assignment, Statement {

    public Instruction(int offset) {
        super(offset);
    }

}
