package io.github.arkosammy12.core.lexer;

import io.github.arkosammy12.core.token.Token;

public sealed interface LexerResult {

    record Ok(SourceStream<Token> tokenStream) implements LexerResult {}

    record Error(String error, SourcePosition sourcePosition) implements LexerResult {}

}
