package io.github.arkosammy12.core.parser;

import io.github.arkosammy12.core.codegen.CodeElement;
import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.parser.directive.LabelDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public sealed interface ParserResult {

    record Ok(Collection<CodeElement> codeElements, Map<String, LabelDefinition> labelDefinitions, Map<Integer, Integer> addressedLabelDefinitions) implements ParserResult {}

    final class Error implements ParserResult {

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
