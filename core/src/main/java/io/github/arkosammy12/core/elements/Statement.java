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
        NativeCallStatement,
        ReturnStatement,
        SaveFlagsStatement,
        SaveRegistersStatement,
        ScrollDownStatement,
        ScrollLeftStatement,
        ScrollRightStatement,
        ScrollUpStatement,
        SetBitplanesStatement,
        SpriteStatement {

    public Statement(int offset) {
        super(offset);
    }

}
