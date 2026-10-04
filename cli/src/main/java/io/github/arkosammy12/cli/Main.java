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
                : main
                v1 := v2
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
                    case ParserResult.Ok(Collection<CodeElement> codeElements, Map<String, LabelDefinition> labelDefinitions, Map<Integer, Integer> addressedLabelDefinitions) -> {
                        for (CodeElement codeElement : codeElements) {
                            IO.println(codeElement);
                        }
                    }
                    case ParserResult.Error parserError -> IO.println("""
                    Assembly error!
                    %s %s
                    """.formatted(parserError.getSourcePosition().map(position -> "(%d:%d)".formatted(position.row() + 1, position.column() + 1)).orElse(""), parserError.getError()));
                }
            }
            case LexerResult.Error(String error, SourcePosition sourcePosition) -> IO.println("""
                    Assembly error!
                    (%d:%d) %s
                    """.formatted(sourcePosition.row() + 1, sourcePosition.column() + 1, error));
        }
    }

}
