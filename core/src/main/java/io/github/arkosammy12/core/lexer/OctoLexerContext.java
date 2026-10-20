package io.github.arkosammy12.core.lexer;

import io.github.arkosammy12.core.result.OctoAssemblerException;
import io.github.arkosammy12.core.token.Token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class OctoLexerContext {

    private final SourceStream<SourceCharacter> characterStream;
    private final List<Token> tokens = new ArrayList<>();
    private final StringBuilder tokenBuilder = new StringBuilder();
    private TokenizerState state = TokenizerState.WHITESPACE;
    private SourcePosition tokenPosition = new SourcePosition(0, 0);
    private int currentRow = 0;

    OctoLexerContext(SourceStream<SourceCharacter> characterStream) {
        this.characterStream = characterStream;
    }

    SourceStream<SourceCharacter> getCharacterStream() {
        return this.characterStream;
    }

    Collection<Token> getTokens() {
        return List.copyOf(this.tokens);
    }

    StringBuilder getTokenBuilder() {
        return this.tokenBuilder;
    }

    TokenizerState getState() {
        return this.state;
    }

    SourcePosition getTokenPosition() {
        return this.tokenPosition;
    }

    void setCurrentRow(int row) {
        this.currentRow = row;
    }

    int getCurrentRow() {
        return this.currentRow;
    }

    void appendCharacter(SourceCharacter sourceCharacter) {
        this.tokenBuilder.append(sourceCharacter.character());
    }

    void beginToken(SourceCharacter sourceCharacter) {
        this.tokenBuilder.setLength(0);
        this.appendCharacter(sourceCharacter);
        this.state = sourceCharacter.character() == '\"' ? TokenizerState.STRING_TOKEN : TokenizerState.TOKEN;
        this.tokenPosition = sourceCharacter.sourcePosition();
    }

    void finishToken() throws OctoAssemblerException {
        String lexeme = this.tokenBuilder.toString();
        if (!lexeme.isBlank()) {
            this.tokens.add(Token.tryParse(lexeme, this.tokenPosition));
        }
        this.state = TokenizerState.WHITESPACE;
        this.tokenBuilder.setLength(0);
    }

    enum TokenizerState {
        WHITESPACE,
        TOKEN,
        STRING_TOKEN
    }

}
