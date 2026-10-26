package io.github.arkosammy12.core.lexer;

import io.github.arkosammy12.core.result.OctoAssemblerException;
import io.github.arkosammy12.core.result.OctoLexerResult;

import java.util.Optional;

import static io.github.arkosammy12.core.lexer.OctoLexerContext.TokenizerState.STRING_TOKEN;

public class OctoLexer {

    public OctoLexerResult lex(SourceStream<SourceCharacter> characterStream) {
        try {
            OctoLexerContext lexerContext = new OctoLexerContext(characterStream);
            while (!lexerContext.getCharacterStream().isEmpty()) {
                Optional<SourceCharacter> sourceCharacter = characterStream.poll();
                if (sourceCharacter.isPresent()) {
                    this.consumeSourceCharacter(lexerContext, sourceCharacter.get());
                }
            }
            return switch (lexerContext.getState()) {
                case STRING_TOKEN -> new OctoLexerResult.Error("Unclosed string literal!", lexerContext.getTokenPosition());
                case TOKEN, WHITESPACE -> {
                    lexerContext.finishToken();
                    yield new OctoLexerResult.Ok(new SourceStream<>(lexerContext.getTokens()));
                }
            };
        } catch (OctoAssemblerException e) {
            return e.toOctoLexerResult();
        }
    }

    private void consumeSourceCharacter(OctoLexerContext octoLexerContext, SourceCharacter newSourceCharacter) throws OctoAssemblerException {
        // Finish the pending token once we have moved on to the next row, if we have one
        if (newSourceCharacter.sourcePosition().row() > octoLexerContext.getCurrentRow()) {
            // Only finish a pending token if it is a regular token. If it is a string token, then we can continue building it on the following row
            if (octoLexerContext.getState() == OctoLexerContext.TokenizerState.TOKEN) {
                octoLexerContext.finishToken();
            }
            octoLexerContext.setCurrentRow(newSourceCharacter.sourcePosition().row());
        }

        char newCharacter = newSourceCharacter.character();
        switch (newCharacter) {
            case '#' -> {
                // If we are building a string token, append the '#'. Otherwise, discard the rest of the row
                if (octoLexerContext.getState() == STRING_TOKEN) {
                    octoLexerContext.appendCharacter(newSourceCharacter);
                } else {
                    octoLexerContext.getCharacterStream().pollUntil(sc -> sc.sourcePosition().row() > octoLexerContext.getCurrentRow());
                }
            }
            case '\"' -> {
                switch (octoLexerContext.getState()) {
                    case WHITESPACE -> octoLexerContext.beginToken(newSourceCharacter); // If going from whitespace to a '"', begin a new string token
                    case TOKEN -> {
                        // If going from a regular token to a quote, finish the previous token, and begin a new string quote
                        octoLexerContext.finishToken();
                        octoLexerContext.beginToken(newSourceCharacter);
                    }
                    case STRING_TOKEN -> {
                        // Add this quote, and if it is not escaped, finish the current string token
                        int backslashRun = 0;
                        for (int i = octoLexerContext.getTokenBuilder().length() - 1; i >= 0 && octoLexerContext.getTokenBuilder().charAt(i) == '\\'; i--) {
                            backslashRun++;
                        }
                        // We should have an uneven amount of preceding contiguous backslashes to guarantee that the first one we encountered corresponds to escaping the current '"'
                        boolean escaped = backslashRun % 2 == 1;
                        octoLexerContext.appendCharacter(newSourceCharacter);
                        if (!escaped) {
                            octoLexerContext.finishToken();
                        }
                    }
                }
            }
            default -> {
                if (isOctoWhitespace(newCharacter)) {
                    switch (octoLexerContext.getState()) {
                        case WHITESPACE -> {} // Do nothing if going from whitespace to whitespace :v
                        case TOKEN -> octoLexerContext.finishToken(); // Finish the current token if going from token to whitespace
                        case STRING_TOKEN -> octoLexerContext.appendCharacter(newSourceCharacter); // Append the whitespace to the current string token
                    }
                } else {
                    switch (octoLexerContext.getState()) {
                        case WHITESPACE -> octoLexerContext.beginToken(newSourceCharacter); // Begin a new token if going from whitespace to non-whitespace
                        case TOKEN, STRING_TOKEN -> octoLexerContext.appendCharacter(newSourceCharacter); // Append the current non-whitespace character to the current token
                    }
                }
            }
        }
    }

    public static boolean isOctoWhitespace(char c) {
        return switch (c) {
            case ' ', '\t', '\r', '\n' -> true;
            default -> false;
        };
    }

}
