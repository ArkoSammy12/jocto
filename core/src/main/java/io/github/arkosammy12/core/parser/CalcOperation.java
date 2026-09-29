package io.github.arkosammy12.core.parser;

public enum CalcOperation {
    MINUS(Type.BOTH),

    BITWISE_NOT(Type.UNARY),
    LOGICAL_NOT(Type.UNARY),
    SIN(Type.UNARY),
    COS(Type.UNARY),
    TAN(Type.UNARY),
    EXP(Type.UNARY),
    LOG(Type.UNARY),
    ABS(Type.UNARY),
    SQRT(Type.UNARY),
    SIGN(Type.UNARY),
    CEIL(Type.UNARY),
    FLOOR(Type.UNARY),
    AT(Type.UNARY),
    STRLEN(Type.UNARY),

    PLUS(Type.BINARY),
    MULTIPLY(Type.BINARY),
    DIVIDE(Type.BINARY),
    MODULO(Type.BINARY),
    BITWISE_AND(Type.BINARY),
    BITWISE_OR(Type.BINARY),
    BITWISE_XOR(Type.BINARY),
    LEFT_SHIFT(Type.BINARY),
    RIGHT_SHIFT(Type.BINARY),
    POW(Type.BINARY),
    MIN(Type.BINARY),
    MAX(Type.BINARY),
    LESS_THAN(Type.BINARY),
    LESS_THAN_THAN_OR_EQUALS(Type.BINARY),
    EQUALS(Type.BINARY),
    NOT_EQUALS(Type.BINARY),
    GREATER_THAN_OR_EQUALS(Type.BINARY),
    GREATER_THAN(Type.BINARY);

    private final Type type;

    CalcOperation(Type type) {
        this.type = type;
    }

    public Type getType() {
        return this.type;
    }

    public enum Type {
        UNARY,
        BINARY,
        BOTH
    }

}
