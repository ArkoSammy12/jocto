package io.github.arkosammy12.core.elements;

public sealed abstract class Assignment extends InstructionElement permits
        AddConstantToRegisterAssignment,
        AddRegisterToRegisterAssignment,
        BitwiseANDRegistersAssignment,
        BitwiseORRegistersAssignment,
        BitwiseXORRegistersAssignment,
        AddRegisterToIndexRegisterAssignment,
        LeftShiftRegisterAssignment,
        LeftSubtractRegisterFromRegisterAssignment,
        RightShiftRegisterAssignment,
        RightSubtractRegisterFromRegisterAssignment,
        SetSoundTimerAssignment,
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

    @Override
    public String toString() {
        return "Assignment[%s]".formatted(this.getStringBaseContents());
    }

}
