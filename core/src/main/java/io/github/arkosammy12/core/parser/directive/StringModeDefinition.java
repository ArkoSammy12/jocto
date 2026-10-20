package io.github.arkosammy12.core.parser.directive;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.result.OctoAssemblerException;
import io.github.arkosammy12.core.token.IntegerLiteralToken;
import io.github.arkosammy12.core.token.StringLiteralToken;
import io.github.arkosammy12.core.token.Token;

import java.util.*;

public final class StringModeDefinition extends ExpandableDirectiveDefinition {

    private final Map<StringLiteralToken, Collection<Token>> alphabets = new HashMap<>();
    private int calls;

    public StringModeDefinition(String name, StringLiteralToken alphabet, Collection<Token> tokenBody) {
        super(name);
        this.alphabets.put(alphabet, tokenBody);
    }

    @Override
    public int getExpansionArgumentQuantity() {
        return 1;
    }

    public void addAlphabet(StringModeDefinition other, SourcePosition sourcePosition) throws OctoAssemblerException {
        for (Map.Entry<StringLiteralToken, Collection<Token>> existingAlphabet : this.alphabets.entrySet()) {
            for (Map.Entry<StringLiteralToken, Collection<Token>> existingOtherAlphabet : other.alphabets.entrySet()) {
                if (existingAlphabet.getKey().getResolved().chars().anyMatch(c -> existingOtherAlphabet.getKey().getResolved().indexOf(c) >= 0)) {
                    throw new OctoAssemblerException("The alphabet '%s' overlaps with the existing alphabet '%s' for the '%s' ':stringmode' directive!".formatted(existingOtherAlphabet.getKey().getLexeme(), existingAlphabet.getKey().getLexeme(), this.name), sourcePosition);
                }
            }
        }
        this.alphabets.putAll(other.alphabets);
    }

    @Override
    public List<Token> expand(int currentOffset, SourcePosition sourcePosition, Token... expansionArguments) throws OctoAssemblerException {
        if (expansionArguments.length != 1) {
            throw new OctoAssemblerException("Wrong number of ':stringmode' invocation arguments: %d!".formatted(expansionArguments.length), sourcePosition);
        }
        if (!(expansionArguments[0] instanceof StringLiteralToken stringLiteralToken)) {
            throw new OctoAssemblerException("The ':stringmode' invocation argument '%s' is not a string literal!".formatted(expansionArguments[0].getLexeme()), expansionArguments[0].getSourcePosition());
        }

        Collection<Token> expandedTokens = new ArrayList<>();
        String resolved = stringLiteralToken.getResolved();
        for (int index = 0; index < resolved.length(); index++) {
            char c = resolved.charAt(index);

            Collection<Token> tokenBody = null;
            int value = -1;
            for (Map.Entry<StringLiteralToken, Collection<Token>> existingAlphabet : this.alphabets.entrySet()) {
                value = existingAlphabet.getKey().getResolved().indexOf(c);
                if (value >= 0) {
                    tokenBody = existingAlphabet.getValue();
                    break;
                }
            }
            if (value < 0 || tokenBody == null) {
                throw new OctoAssemblerException("The character '%s' is not defined in an alphabet for the '%s' ':stringmode' directive!".formatted(String.valueOf(c), this.name));
            }

            for (Token bodyToken : tokenBody) {
                String lexeme = bodyToken.getLexeme();
                switch (lexeme) {
                    case "CALLS" -> expandedTokens.add(new IntegerLiteralToken(this.calls, lexeme, bodyToken.getSourcePosition()));
                    case "CHAR" -> expandedTokens.add(new IntegerLiteralToken((byte) c, lexeme, bodyToken.getSourcePosition()));
                    case "INDEX" -> expandedTokens.add(new IntegerLiteralToken(index, lexeme, bodyToken.getSourcePosition()));
                    case "VALUE" -> expandedTokens.add(new IntegerLiteralToken(value, lexeme, bodyToken.getSourcePosition()));
                    default -> expandedTokens.add(bodyToken);
                }
            }

            this.calls++;
            if (this.calls < 0) {
                throw new OctoAssemblerException("Exceeded maximum amount of macro invocations of %d for ':stringmode' macro '%s'".formatted(Integer.MAX_VALUE, this.name), sourcePosition);
            }
        }
        return List.copyOf(expandedTokens);
    }

}
