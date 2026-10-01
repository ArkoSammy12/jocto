package io.github.arkosammy12.core.codegen;

public sealed abstract class Assignment extends Instruction permits
        AddConstantToRegisterAssignment,
        AddRegisterToRegisterAssignment,
        BitwiseAndRegisterAssignment,
        BitwiseOrRegisterAssignment,
        BitwiseXorRegisterAssignment,
        CopyRegisterAssignment,
        IncrementIndexRegisterAssignment,
        LeftShiftRegisterAssignment,
        LeftSubtractRegisterFromRegisterAssignment,
        RightShiftRegisterAssignment,
        RightSubtractRegisterFromRegisterAssignment,
        SetBuzzerTimerAssignment,
        SetDelayTimerAssignment,
        SetIndexRegisterToBigHexCharAssignment,
        SetIndexRegisterToConstantAssignment,
        SetIndexRegisterToHexCharAssignment,
        SetIndexRegisterToLongConstantAssignment,
        SetPitchAssignment,
        SetRegisterToConstantAssignment,
        SetRegisterToDelayTimerAssignment,
        SetRegisterToKeyAssignment,
        SetRegisterToRandomAssignment,
        SetRegisterToRegisterAssignment {

    public Assignment(int offset) {
        super(offset);
    }

}
