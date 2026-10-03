package io.github.arkosammy12.core.lexer;


import io.github.arkosammy12.core.token.Token;

import java.util.ArrayList;
import java.util.Collection;
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
            if (newSourceCharacter.sourcePosition().row() > tokenizerContext.getCurrentRow()) {
                // Only finish a pending token if it is a regular token. If it is a string token, then we can continue building it on the following row
                if (tokenizerContext.getState() == TokenizerState.TOKEN) {
                    tokenizerContext.finishToken();
                }
                tokenizerContext.setCurrentRow(newSourceCharacter.sourcePosition().row());
            }

            char newCharacter = newSourceCharacter.character();
            switch (newCharacter) {
                case '#' -> {
                    // If we are building a string token, append the '#'. Otherwise, discard the rest of the row
                    if (tokenizerContext.getState() == TokenizerState.STRING_TOKEN) {
                        tokenizerContext.appendCharacter(newSourceCharacter);
                    } else {
                        characterStream.pollUntil(sc -> sc.sourcePosition().row() > tokenizerContext.getCurrentRow());
                    }
                }
                case '\"' -> {
                    switch (tokenizerContext.getState()) {
                        case WHITESPACE -> tokenizerContext.beginToken(newSourceCharacter); // If going from whitespace to a '"', begin a new string token
                        case TOKEN -> {
                            // If going from a regular token to a quote, finish the previous token, and begin a new string quote
                            tokenizerContext.finishToken();
                            tokenizerContext.beginToken(newSourceCharacter);
                        }
                        case STRING_TOKEN -> {
                            // Add this quote, and if it is not escaped, finish the current string token
                            int backslashRun = 0;
                            for (int i = tokenizerContext.getTokenBuilder().length() - 1; i >= 0 && tokenizerContext.getTokenBuilder().charAt(i) == '\\'; i--) {
                                backslashRun++;
                            }
                            // We should have an uneven amount of preceding contiguous backslashes to guarantee that the first one we encountered corresponds to escaping the current '"'
                            boolean escaped = backslashRun % 2 == 1;
                            tokenizerContext.appendCharacter(newSourceCharacter);
                            if (!escaped) {
                                tokenizerContext.finishToken();
                            }
                        }
                    }
                }
                default -> {
                    if (Character.isWhitespace(newCharacter)) {
                        switch (tokenizerContext.getState()) {
                            case WHITESPACE -> {} // Do nothing if going from whitespace to whitespace :v
                            case TOKEN -> tokenizerContext.finishToken(); // Finish the current token if going from token to whitespace
                            case STRING_TOKEN -> tokenizerContext.appendCharacter(newSourceCharacter); // Append the whitespace to the current string token
                        }
                    } else {
                        switch (tokenizerContext.getState()) {
                            case WHITESPACE -> tokenizerContext.beginToken(newSourceCharacter); // Begin a new token if going from whitespace to non-whitespace
                            case TOKEN, STRING_TOKEN -> tokenizerContext.appendCharacter(newSourceCharacter); // Append the current non-whitespace character to the current token
                        }
                    }
                }
            }
        }

        return switch (tokenizerContext.getState()) {
            case STRING_TOKEN -> new LexerResult.Error("Unclosed string literal!", tokenizerContext.getTokenPosition());
            case TOKEN, WHITESPACE -> {
                tokenizerContext.finishToken();
                yield new LexerResult.Ok(new SourceStream<>(tokenizerContext.getTokens()));
            }
        };
    }


    private static class TokenizerContext {

        private final List<Token> tokens = new ArrayList<>();
        private final StringBuilder tokenBuilder = new StringBuilder();
        private TokenizerState state = TokenizerState.WHITESPACE;
        private SourcePosition tokenPosition = new SourcePosition(0, 0);
        private int currentRow = 0;

        private Collection<Token> getTokens() {
            return List.copyOf(this.tokens);
        }

        private StringBuilder getTokenBuilder() {
            return this.tokenBuilder;
        }

        private TokenizerState getState() {
            return this.state;
        }

        private SourcePosition getTokenPosition() {
            return this.tokenPosition;
        }

        private void setCurrentRow(int row) {
            this.currentRow = row;
        }

        private int getCurrentRow() {
            return this.currentRow;
        }

        private void appendCharacter(SourceCharacter sourceCharacter) {
            this.tokenBuilder.append(sourceCharacter.character());
        }

        private void beginToken(SourceCharacter sourceCharacter) {
            this.tokenBuilder.setLength(0);
            this.appendCharacter(sourceCharacter);
            this.state = sourceCharacter.character() == '\"' ? TokenizerState.STRING_TOKEN : TokenizerState.TOKEN;
            this.tokenPosition = sourceCharacter.sourcePosition();
        }

        private void finishToken() {
            String lexeme = this.tokenBuilder.toString();
            if (!lexeme.isBlank()) {
                this.tokens.add(Token.tryParse(lexeme, this.tokenPosition));
            }
            this.state = TokenizerState.WHITESPACE;
            this.tokenBuilder.setLength(0);
        }

    }

    private enum TokenizerState {
        WHITESPACE,
        TOKEN,
        STRING_TOKEN
    }

}
