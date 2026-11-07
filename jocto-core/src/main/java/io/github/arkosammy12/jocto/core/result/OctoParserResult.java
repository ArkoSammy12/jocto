package io.github.arkosammy12.jocto.core.result;

import org.jetbrains.annotations.Nullable;

import io.github.arkosammy12.jocto.core.elements.CodeElement;
import io.github.arkosammy12.jocto.core.lexer.SourcePosition;
import io.github.arkosammy12.jocto.core.parser.InternalLabelKey;
import io.github.arkosammy12.jocto.core.parser.directive.LabelDefinition;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public sealed interface OctoParserResult extends OctoAssemblerResult {

    record Ok(Collection<CodeElement> codeElements, Map<String, LabelDefinition> labelDefinitions, Map<InternalLabelKey, Integer> internalLabelDefinitions) implements OctoParserResult, OctoAssemblerResult.Ok {

        @Override
        public String getString() {
            return "OctoParserResult.Ok[codeElements=%d, labelDefinitions=%d, internalLabelDefinitions=%d]".formatted(this.codeElements().size(), this.labelDefinitions().size(), this.internalLabelDefinitions().size());
        }

    }

    final class Error implements OctoParserResult, OctoAssemblerResult.Error {

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
