package io.github.arkosammy12.cli;

import io.github.arkosammy12.core.codegen.CodeElement;
import io.github.arkosammy12.core.lexer.*;
import io.github.arkosammy12.core.parser.OctoParser;
import io.github.arkosammy12.core.parser.ParserResult;
import io.github.arkosammy12.core.parser.directive.LabelDefinition;
import io.github.arkosammy12.core.token.Token;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Main {

    static void main(String[] args) {
        String test = """
                if v1 == 0x00 begin
                    v1 := v2
                else
                    v3 := v4
                end
                """;

        SourceFile sourceFile = new SourceFile(List.of(test.split("\n")));
        OctoLexer octoLexer = new OctoLexer();
        LexerResult lexerResult = octoLexer.tokenize(sourceFile.createSourceCharacterStream());
        switch (lexerResult) {
            case LexerResult.Ok(SourceStream<Token> tokenStream) -> {
                tokenStream.forEach(IO::println);
                OctoParser octoParser = new OctoParser();
                ParserResult parserResult = octoParser.parseTokens(tokenStream);
                switch (parserResult) {
                    case ParserResult.Ok(Collection<CodeElement> codeElements, Map<String, LabelDefinition> labelDefinitions) -> {
                        for (CodeElement codeElement :codeElements) {
                            IO.println(codeElement);
                        }
                    }
                    case ParserResult.Error(String error, SourcePosition sourcePosition) -> IO.println("""
                    Assembly error!
                    (%d:%d) %s
                    """.formatted(sourcePosition.row() + 1, sourcePosition.column() + 1, error));
                }
            }
            case LexerResult.Error(String error, SourcePosition sourcePosition) -> IO.println("""
                    Assembly error!
                    (%d:%d) %s
                    """.formatted(sourcePosition.row() + 1, sourcePosition.column() + 1, error));
        }
    }

}
