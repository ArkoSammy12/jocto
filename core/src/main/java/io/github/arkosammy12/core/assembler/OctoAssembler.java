package io.github.arkosammy12.core.assembler;

import io.github.arkosammy12.core.lexer.OctoLexer;
import io.github.arkosammy12.core.lexer.SourceFile;
import io.github.arkosammy12.core.parser.OctoParser;
import io.github.arkosammy12.core.result.OctoAssemblerResult;
import io.github.arkosammy12.core.result.OctoLexerResult;
import io.github.arkosammy12.core.result.OctoParserResult;

public class OctoAssembler {

    private final int programStart;
    private final OctoAssemblyStage stopAfterAssemblyStage;

    public OctoAssembler(int programStart, OctoAssemblyStage stopAfterAssemblyStage) {
        if (programStart < 0) {
            throw new IllegalArgumentException("The program start value cannot be less than zero!");
        }
        this.programStart = programStart;
        this.stopAfterAssemblyStage = stopAfterAssemblyStage;
    }

    public OctoAssembler(int programStart) {
        this(programStart, OctoAssemblyStage.CODEGEN);
    }

    public OctoAssemblerResult assemble(SourceFile sourceFile) {
        OctoLexer octoLexer = new OctoLexer();
        return switch (octoLexer.tokenize(sourceFile.createSourceCharacterStream())) {
            case OctoLexerResult.Error lexerError ->  lexerError;
            case OctoLexerResult.Ok lexerOk -> {
                if (this.stopAfterAssemblyStage == OctoAssemblyStage.LEXING) {
                    yield lexerOk;
                }
                OctoParser octoParser = new OctoParser(this.programStart);
                yield switch (octoParser.parseTokens(lexerOk.tokenStream())) {
                    case OctoParserResult.Error parserError -> parserError;
                    case OctoParserResult.Ok parserOk -> {
                        if (this.stopAfterAssemblyStage == OctoAssemblyStage.PARSING) {
                            yield parserOk;
                        }
                        OctoCodegen octoCodegen = new OctoCodegen(this.programStart);
                        yield octoCodegen.generateCode(parserOk.codeElements(), parserOk.labelDefinitions(), parserOk.internalLabelDefinitions());
                    }
                };
            }
        };
    }

    public static String byteArrayToString(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i == 0) {
                stringBuilder.append("%02X".formatted(bytes[i] & 0xFF));
            } else {
                stringBuilder.append(" %02X".formatted(bytes[i] & 0xFF));
            }
        }
        return stringBuilder.toString();
    }

}
