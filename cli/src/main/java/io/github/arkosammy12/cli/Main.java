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
                ###########################################
                #
                #  Tank
                #
                #  Classic Chip8 program translated from
                #  VIPer Volume 1 Issue 1 (June 1978), pg 12-14
                #  https://github.com/mattmikolay/viper/blob/master/volume1/issue1.pdf
                #
                #  Press 2/E/S/Q to move the tank.
                #
                ###########################################
               
                
                : up    v2 += -1  i := tankup    ;
                : down  v2 +=  1  i := tankdown  ;
                : right v1 += -1  i := tankright ;
                : left  v1 +=  1  i := tankleft  ;
                
                : main
                  v1 := 0x20
                  v2 := 0x10
                  i := tankup
                
                    sprite v1 v2 7
                    v0 := key
                    sprite v1 v2 7
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
