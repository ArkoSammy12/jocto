package io.github.arkosammy12.core.result;

import io.github.arkosammy12.core.lexer.SourcePosition;

import java.util.Optional;

public sealed interface OctoAssemblerResult permits OctoAssemblerResult.Error, OctoAssemblerResult.Ok, OctoCodegenResult, OctoLexerResult, OctoParserResult {

    sealed interface Ok extends OctoAssemblerResult permits OctoCodegenResult.Ok, OctoLexerResult.Ok, OctoParserResult.Ok {}

    sealed interface Error extends OctoAssemblerResult permits OctoCodegenResult.Error, OctoLexerResult.Error, OctoParserResult.Error {

        String getError();

        Optional<SourcePosition> getSourcePosition();

    }

}
