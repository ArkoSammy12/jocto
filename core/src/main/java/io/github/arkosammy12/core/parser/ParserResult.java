package io.github.arkosammy12.core.parser;

import io.github.arkosammy12.core.codegen.CodeElement;
import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.parser.directive.LabelDefinition;

import java.util.Collection;
import java.util.Map;

public sealed interface ParserResult {

    record Ok(Collection<CodeElement> codeElements, Map<String, LabelDefinition> labelDefinitions) implements ParserResult {}

    record Error(String error, SourcePosition sourcePosition) implements ParserResult {}

}
