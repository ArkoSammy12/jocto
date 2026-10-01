package io.github.arkosammy12.core.codegen;

public sealed abstract class Statement extends Instruction permits
        AudioStatement,
        BCDStatement,
        CallStatement,
        ClearStatement,
        ExitStatement,
        HiresStatement,
        JumpStatement,
        JumpZeroStatement,
        LoadFlagsStatement,
        LoadRegistersRangeStatement,
        LoadRegistersStatement,
        LoresStatement,
        PlaneStatement,
        ReturnStatement,
        SaveFlagsStatement,
        SaveRegistersRangeStatement,
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
