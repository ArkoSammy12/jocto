package io.github.arkosammy12.core.parser.directive;

import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.result.OctoAssemblerException;
import io.github.arkosammy12.core.token.IntegerLiteralToken;
import io.github.arkosammy12.core.token.Token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class MacroDefinition extends DirectiveDefinition implements ExpandableDirective {

    private final List<Token> arguments;
    private final Collection<Token> tokenBody;
    private int calls = 0;

    public MacroDefinition(String name, List<Token> arguments, Collection<Token> tokenBody) {
        super(name);
        this.arguments = List.copyOf(arguments);
        this.tokenBody = List.copyOf(tokenBody);
    }

    @Override
    public int getExpansionArgumentQuantity() {
        return this.arguments.size();
    }

    @Override
    public List<Token> expand(int currentOffset, SourcePosition sourcePosition, Token... expansionArguments) throws OctoAssemblerException {
        if (expansionArguments.length != arguments.size()) {
            throw new OctoAssemblerException("Expected %d arguments for '%s' macro invocation!".formatted(this.tokenBody.size(), this.name), sourcePosition);
        }
        Collection<Token> expandedTokens = new ArrayList<>();

        expansionLoop: for (Token bodyToken : this.tokenBody) {
            for (int i = 0; i < this.arguments.size(); i++) {
                Token argumentToken = this.arguments.get(i);
                if (Objects.equals(argumentToken.getLexeme(), bodyToken.getLexeme())) {
                    // TODO: Consider updating the argument token's source position to match the macro invocation's site for better error reporting
                    expandedTokens.add(expansionArguments[i]);
                    continue expansionLoop;
                }
            }
            if ("CALLS".equals(bodyToken.getLexeme())) {
                expandedTokens.add(new IntegerLiteralToken(this.calls, "CALLS", sourcePosition));
                continue;
            }
            expandedTokens.add(bodyToken);
        }

        this.calls++;
        if (this.calls < 0) {
            throw new OctoAssemblerException("Exceeded maximum amount of macro invocations of %d for macro '%s'".formatted(Integer.MAX_VALUE, this.name), sourcePosition);
        }
        return List.copyOf(expandedTokens);
    }

}
