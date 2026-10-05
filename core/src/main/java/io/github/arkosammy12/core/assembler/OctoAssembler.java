package io.github.arkosammy12.core.assembler;

import io.github.arkosammy12.core.elements.CodeElement;
import io.github.arkosammy12.core.lexer.OctoLexer;
import io.github.arkosammy12.core.lexer.SourceFile;
import io.github.arkosammy12.core.lexer.SourceStream;
import io.github.arkosammy12.core.parser.InternalLabelKey;
import io.github.arkosammy12.core.parser.OctoParser;
import io.github.arkosammy12.core.parser.directive.LabelDefinition;
import io.github.arkosammy12.core.result.OctoAssemblerResult;
import io.github.arkosammy12.core.result.OctoCodegenResult;
import io.github.arkosammy12.core.result.OctoLexerResult;
import io.github.arkosammy12.core.result.OctoParserResult;
import io.github.arkosammy12.core.token.Token;

import java.util.Collection;
import java.util.Map;

public class OctoAssembler {

    private final int programStart;

    public OctoAssembler(int programStart) {
        if (programStart < 0) {
            throw new IllegalArgumentException("The program start value cannot be less than zero!");
        }
        this.programStart = programStart;
    }

    public OctoAssemblerResult assemble(SourceFile sourceFile) {
        OctoLexer octoLexer = new OctoLexer();
        return switch (octoLexer.tokenize(sourceFile.createSourceCharacterStream())) {
            case OctoLexerResult.Error lexerError ->  lexerError;
            case OctoLexerResult.Ok(SourceStream<Token> tokenStream) -> {
                OctoParser octoParser = new OctoParser(this.programStart);
                yield switch (octoParser.parseTokens(tokenStream)) {
                    case OctoParserResult.Error parserError -> parserError;
                    case OctoParserResult.Ok(Collection<CodeElement> codeElements, Map<String, LabelDefinition> labelDefinitions, Map<InternalLabelKey, Integer> internalLabelDefinitions) -> {
                        OctoCodegen octoCodegen = new OctoCodegen(this.programStart);
                        yield octoCodegen.generateCode(codeElements, labelDefinitions, internalLabelDefinitions);
                    }
                };
            }
        };
    }

}
