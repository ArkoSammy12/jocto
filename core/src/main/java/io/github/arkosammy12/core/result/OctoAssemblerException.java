package io.github.arkosammy12.core.result;

import io.github.arkosammy12.core.lexer.SourcePosition;
import org.jetbrains.annotations.Nullable;

public class OctoAssemblerException extends Exception {

    private final String error;

    @Nullable
    private final SourcePosition sourcePosition;

    public OctoAssemblerException(String error, @Nullable SourcePosition sourcePosition) {
        super(error);
        this.error = error;
        this.sourcePosition = sourcePosition;
    }

    public OctoAssemblerException(String error) {
        this(error, null);
    }

    public OctoParserResult toParserResult() {
        return new OctoParserResult.Error(this.error, this.sourcePosition);
    }

    public OctoCodegenResult toCodegenResult() {
        return new OctoCodegenResult.Error(this.error, this.sourcePosition);
    }

}
