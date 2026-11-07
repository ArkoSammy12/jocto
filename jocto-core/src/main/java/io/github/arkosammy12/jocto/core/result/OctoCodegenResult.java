package io.github.arkosammy12.jocto.core.result;

import org.jetbrains.annotations.Nullable;

import io.github.arkosammy12.jocto.core.lexer.SourcePosition;

import java.util.Optional;

public sealed interface OctoCodegenResult extends OctoAssemblerResult {

    record Ok(byte[] rom) implements OctoCodegenResult, OctoAssemblerResult.Ok {

        @Override
        public String getString() {
            return "OctoCodegenResult.Ok[bytes=%d]".formatted(this.rom().length);
        }

    }

    final class Error implements OctoCodegenResult, OctoAssemblerResult.Error {

        private final String error;

        @Nullable
        private final SourcePosition sourcePosition;

        public Error(String error, @Nullable SourcePosition sourcePosition) {
            this.error = error;
            this.sourcePosition = sourcePosition;
        }

        public Error(String error) {
            this(error, null);
        }

        public String getError() {
            return this.error;
        }

        public Optional<SourcePosition> getSourcePosition() {
            return Optional.ofNullable(this.sourcePosition);
        }

    }

}
