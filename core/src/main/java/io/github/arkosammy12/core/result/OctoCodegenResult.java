package io.github.arkosammy12.core.result;

import io.github.arkosammy12.core.lexer.SourcePosition;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public sealed interface OctoCodegenResult extends OctoAssemblerResult {

    record Ok(byte[] rom) implements OctoCodegenResult, OctoAssemblerResult.Ok {}

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
