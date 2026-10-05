package io.github.arkosammy12.core.result;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.lexer.SourceStream;
import io.github.arkosammy12.core.token.Token;

import java.util.Optional;

public sealed interface OctoLexerResult extends OctoAssemblerResult {

    record Ok(SourceStream<Token> tokenStream) implements OctoLexerResult, OctoAssemblerResult.Ok {}

    record Error(String error, SourcePosition sourcePosition) implements OctoLexerResult, OctoAssemblerResult.Error {


        @Override
        public String getError() {
            return this.error();
        }

        @Override
        public Optional<SourcePosition> getSourcePosition() {
            return Optional.of(this.sourcePosition());
        }
    }

}
