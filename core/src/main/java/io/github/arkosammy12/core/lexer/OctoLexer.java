package io.github.arkosammy12.core.lexer;


import io.github.arkosammy12.core.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OctoLexer {

    /// Converts the provided stream of source characters into a stream of tokens suitable for parsing
    ///
    /// @param characterStream The stream of source characters to consume for tokenization
    /// @return A LexerResult, representing a stream of tokens, or a failure message along with the position in the source file where the error was reported
    public LexerResult tokenize(SourceStream<SourceCharacter> characterStream) {
        TokenizerContext tokenizerContext = new TokenizerContext();

        while (!characterStream.isEmpty()) {
            Optional<SourceCharacter> optionalSourceCharacter = characterStream.poll();
            if (optionalSourceCharacter.isEmpty()) {
                continue;
            }
            SourceCharacter newSourceCharacter = optionalSourceCharacter.get();

            // Finish the pending token once we have moved on to the next row, if we have one
            if (newSourceCharacter.sourcePosition().row() > tokenizerContext.currentRow) {
                if (tokenizerContext.state == TokenizerState.STRING_TOKEN) {
                    // Fail on string cutoff by a new line
                    return new LexerResult.Error("Unclosed string literal!", tokenizerContext.tokenPosition);
                }
                this.finishToken(tokenizerContext);
                tokenizerContext.currentRow = newSourceCharacter.sourcePosition().row();
            }

            char newCharacter = newSourceCharacter.character();
            switch (newCharacter) {
                case '#' -> {
                    // If we are building a string token, append the '#'. Otherwise, discard the rest of the row
                    if (tokenizerContext.state == TokenizerState.STRING_TOKEN) {
                        this.appendCharacter(tokenizerContext, newSourceCharacter);
                    } else {
                        characterStream.pollUntil(sc -> sc.sourcePosition().row() > tokenizerContext.currentRow);
                    }
                }
                case '\"' -> {
                    switch (tokenizerContext.state) {
                        case WHITESPACE -> this.beginToken(tokenizerContext, newSourceCharacter); // If going from whitespace to a '"', begin a new string token
                        case TOKEN -> {
                            // If going from a regular token to a quote, finish the previous token, and begin a new string quote
                            this.finishToken(tokenizerContext);
                            this.beginToken(tokenizerContext, newSourceCharacter);
                        }
                        case STRING_TOKEN -> {
                            // Add this quote, and if it is not escaped, finish the current string token
                            int backslashRun = 0;
                            for (int i = tokenizerContext.tokenBuilder.length() - 1; i >= 0 && tokenizerContext.tokenBuilder.charAt(i) == '\\'; i--) {
                                backslashRun++;
                            }
                            // We should have an uneven amount of preceding contiguous backslashes to guarantee that the first one we encountered corresponds to escaping the current '"'
                            boolean escaped = backslashRun % 2 == 1;
                            this.appendCharacter(tokenizerContext, newSourceCharacter);
                            if (!escaped) {
                                this.finishToken(tokenizerContext);
                            }
                        }
                    }
                }
                default -> {
                    if (Character.isWhitespace(newCharacter)) {
                        switch (tokenizerContext.state) {
                            case WHITESPACE -> {} // Do nothing if going from whitespace to whitespace :v
                            case TOKEN -> this.finishToken(tokenizerContext); // Finish the current token if going from token to whitespace
                            case STRING_TOKEN -> this.appendCharacter(tokenizerContext, newSourceCharacter); // Append the whitespace to the current string token
                        }
                    } else {
                        switch (tokenizerContext.state) {
                            case WHITESPACE -> this.beginToken(tokenizerContext, newSourceCharacter); // Begin a new token if going from whitespace to non-whitespace
                            case TOKEN, STRING_TOKEN -> this.appendCharacter(tokenizerContext, newSourceCharacter); // Append the current non-whitespace character to the current token
                        }
                    }
                }
            }
        }

        return switch (tokenizerContext.state) {
            case STRING_TOKEN -> new LexerResult.Error("Unclosed string literal!", tokenizerContext.tokenPosition);
            case TOKEN, WHITESPACE -> {
                this.finishToken(tokenizerContext);
                yield new LexerResult.Ok(new SourceStream<>(tokenizerContext.tokens));
            }
        };
    }

    private void appendCharacter(TokenizerContext tokenizerContext, SourceCharacter sourceCharacter) {
        tokenizerContext.tokenBuilder.append(sourceCharacter.character());
    }

    private void beginToken(TokenizerContext tokenizerContext, SourceCharacter sourceCharacter) {
        tokenizerContext.tokenBuilder.setLength(0);
        this.appendCharacter(tokenizerContext, sourceCharacter);
        tokenizerContext.state = sourceCharacter.character() == '\"' ? TokenizerState.STRING_TOKEN : TokenizerState.TOKEN;
        tokenizerContext.tokenPosition = sourceCharacter.sourcePosition();
    }

    private void finishToken(TokenizerContext tokenizerContext) {
        String lexeme = tokenizerContext.tokenBuilder.toString();
        if (!lexeme.isBlank()) {
            tokenizerContext.tokens.add(Token.tryParse(lexeme, tokenizerContext.tokenPosition));
        }
        tokenizerContext.state = TokenizerState.WHITESPACE;
        tokenizerContext.tokenBuilder.setLength(0);
    }

    private static class TokenizerContext {

        private final List<Token> tokens = new ArrayList<>();
        private int currentRow = 0;
        private TokenizerState state = TokenizerState.WHITESPACE;
        private final StringBuilder tokenBuilder = new StringBuilder();
        private SourcePosition tokenPosition = new SourcePosition(0, 0);

    }

    private enum TokenizerState {
        WHITESPACE,
        TOKEN,
        STRING_TOKEN

    }

}
