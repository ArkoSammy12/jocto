package io.github.arkosammy12.jocto.core.result;

import java.util.Optional;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;
import io.github.arkosammy12.jocto.core.lexer.SourceStream;
import io.github.arkosammy12.jocto.core.token.Token;

public sealed interface OctoLexerResult extends OctoAssemblerResult {

    record Ok(SourceStream<Token> tokenStream) implements OctoLexerResult, OctoAssemblerResult.Ok {

        @Override
        public String getString() {
            return "OctoLexerResult.Ok[]";
        }

    }

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
