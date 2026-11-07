package io.github.arkosammy12.jocto.core.result;

import org.jetbrains.annotations.Nullable;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;

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

    public OctoLexerResult toOctoLexerResult() {
        return new OctoLexerResult.Error(this.error, this.sourcePosition);
    }

    public OctoParserResult toParserResult() {
        return new OctoParserResult.Error(this.error, this.sourcePosition);
    }

    public OctoCodegenResult toCodegenResult() {
        return new OctoCodegenResult.Error(this.error, this.sourcePosition);
    }

}
