package io.github.arkosammy12.core.elements;

public sealed abstract class Statement extends InstructionElement permits
        AudioStatement,
        BCDStatement,
        CallStatement,
        ClearScreenStatement,
        ExitStatement,
        HiresStatement,
        JumpStatement,
        JumpZeroStatement,
        LoadFlagsStatement,
        LoadRegistersStatement,
        LoresStatement,
        SetBitplanesStatement,
        ReturnStatement,
        SaveFlagsStatement,
        SaveRegistersStatement,
        ScrollDownStatement,
        ScrollLeftStatement,
        ScrollRightStatement,
        ScrollUpStatement,
        SpriteStatement {

    public Statement(int offset) {
        super(offset);
    }

}
