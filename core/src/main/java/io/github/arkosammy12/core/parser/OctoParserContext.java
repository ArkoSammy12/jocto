package io.github.arkosammy12.core.parser;

import io.github.arkosammy12.core.elements.CallStatement;
import io.github.arkosammy12.core.elements.CodeElement;
import io.github.arkosammy12.core.elements.LabelableElement;
import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.lexer.SourceStream;
import io.github.arkosammy12.core.parser.directive.*;
import io.github.arkosammy12.core.result.OctoAssemblerException;
import io.github.arkosammy12.core.token.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

class OctoParserContext {

    private final int programStart;
    private final SourceStream<Token> tokenStream;
    private final List<CodeElement> codeElements = new ArrayList<>();
    private final Map<String, NonConstantDirectiveDefinition> nonConstantDirectiveDefinition = new HashMap<>();
    private final Map<String, ConstantDirectiveDefinition> constantDirectiveDefinitionMap = new HashMap<>();
    private final Map<InternalLabelKey, Integer> internalLabelDefinitions = new HashMap<>();
    private final Stack<InternalLabelKey> ifStack = new Stack<>();
    private final Stack<InternalLabelKey> loopStack = new Stack<>();
    private int here;
    private long tokenPollCounter = 0;
    private boolean foundMainLabel;

    {
        for (OctoBuiltInConstants octoBuiltInConstant : OctoBuiltInConstants.values()) {
            this.constantDirectiveDefinitionMap.put(octoBuiltInConstant.getKey(), new ConstDefinition(octoBuiltInConstant.getKey(), octoBuiltInConstant.getValue()));
        }
    }

    OctoParserContext(int programStart, SourceStream<Token> tokenStream) {
        if (programStart < 0) {
            throw new IllegalArgumentException("The program start value cannot be less than zero!");
        }
        this.programStart = programStart;
        this.here = programStart;
        this.tokenStream = tokenStream;
    }

    SourceStream<Token> getTokenStream() {
        return this.tokenStream;
    }

    int getHere() {
        return this.here;
    }

    Optional<ConstantDirectiveDefinition> getConstantDirective(Token token) {
        return this.getConstantDirective(token.getLexeme());
    }

    Optional<ConstantDirectiveDefinition> getConstantDirective(String name) {
        return Optional.ofNullable(this.constantDirectiveDefinitionMap.get(name));
    }

    Optional<NonConstantDirectiveDefinition> getNonConstantDirective(Token token) {
        return this.getNonConstantDirective(token.getLexeme());
    }

    Optional<NonConstantDirectiveDefinition> getNonConstantDirective(String name) {
        return Optional.ofNullable(this.nonConstantDirectiveDefinition.get(name));
    }

    Collection<CodeElement> getCodeElements() {
        return List.copyOf(this.codeElements);
    }

    Map<String, LabelDefinition> getLabelDefinitions() {
        Map<String, LabelDefinition> labelDefinitions = new HashMap<>();
        for (Map.Entry<String, ConstantDirectiveDefinition> entries : this.constantDirectiveDefinitionMap.entrySet()) {
            if (entries.getValue() instanceof LabelDefinition labelDefinition) {
                labelDefinitions.put(entries.getKey(), labelDefinition);
            }
        }
        return Map.copyOf(labelDefinitions);
    }

    Map<InternalLabelKey, Integer> getInternalLabelDefinitions() {
        return Map.copyOf(this.internalLabelDefinitions);
    }

    boolean foundMainLabel() {
        return this.foundMainLabel;
    }

    void addCodeElement(CodeElement codeElement) throws OctoAssemblerException {
        if (codeElement instanceof LabelableElement labelableElement && labelableElement.getAddressArgument() instanceof AddressArgument.NamedLabelReference(Token token)) {
            this.checkReservedName(token);
        }
        this.codeElements.add(codeElement);
    }

    void addDirectiveDefinition(Token token, DirectiveDefinition newDirectiveDefinition) throws OctoAssemblerException {
        this.checkReservedName(token);
        String name = token.getLexeme();
        switch (newDirectiveDefinition) {
            case ConstantDirectiveDefinition newConstantDirectiveDefinition -> {
                Optional<ConstantDirectiveDefinition> existingConstantDirectiveDefinition = this.getConstantDirective(name);
                if (existingConstantDirectiveDefinition.isPresent() && !(existingConstantDirectiveDefinition.get() instanceof CalcDefinition && newConstantDirectiveDefinition instanceof CalcDefinition)) {
                    throw new OctoAssemblerException("Constant directive name '%s' is already defined!".formatted(name), token.getSourcePosition());
                }

                Optional<NonConstantDirectiveDefinition> existingNonConstantDirectiveDefinition = this.getNonConstantDirective(name);
                if (existingNonConstantDirectiveDefinition.isPresent() && existingNonConstantDirectiveDefinition.get() instanceof AliasDefinition && (newConstantDirectiveDefinition instanceof LabelDefinition || newConstantDirectiveDefinition instanceof NextDefinition)) {
                    throw new OctoAssemblerException("The constant directive name '%s' is already defined as an ':alias'!".formatted(name), token.getSourcePosition());
                }

                if (newConstantDirectiveDefinition instanceof LabelDefinition labelDefinition && "main".equals(labelDefinition.getName())) {
                    this.foundMainLabel = true;
                    if (labelDefinition.getAddress() == this.programStart + 2 || labelDefinition.getAddress() == this.programStart) {
                        this.here = this.programStart;
                        this.codeElements.clear();
                        newConstantDirectiveDefinition = new LabelDefinition("main", this.programStart);
                    }
                }
                this.constantDirectiveDefinitionMap.put(name, newConstantDirectiveDefinition);
            }
            case NonConstantDirectiveDefinition newNonConstantDirectiveDefinition -> {
                Optional<NonConstantDirectiveDefinition> existingNonConstantDirectiveDefinition = this.getNonConstantDirective(name);
                if (newNonConstantDirectiveDefinition instanceof StringModeDefinition newStringModeDefinition && existingNonConstantDirectiveDefinition.isPresent() && existingNonConstantDirectiveDefinition.get() instanceof StringModeDefinition existingStringModeDefinition) {
                    existingStringModeDefinition.addAlphabet(newStringModeDefinition, token.getSourcePosition());
                    return;
                }
                if (existingNonConstantDirectiveDefinition.isPresent() && !(newNonConstantDirectiveDefinition instanceof AliasDefinition && existingNonConstantDirectiveDefinition.get() instanceof AliasDefinition)) {
                    throw new OctoAssemblerException("Directive name '%s' is already defined!".formatted(name), token.getSourcePosition());
                }
                if (this.getConstantDirective(name).isPresent() && newNonConstantDirectiveDefinition instanceof AliasDefinition) {
                    throw new OctoAssemblerException("The ':alias' directive name '%s' is already defined as a constant!".formatted(name), token.getSourcePosition());
                }
                this.nonConstantDirectiveDefinition.put(name, newNonConstantDirectiveDefinition);
            }
        }
    }

    void addInternalLabelDefinition(InternalLabelKey internalLabelKey, int addressValue) {
        if (this.internalLabelDefinitions.containsKey(internalLabelKey) && this.internalLabelDefinitions.get(internalLabelKey) != addressValue) {
            throw new IllegalArgumentException("Tried to insert internal label definition with key '%s' because it is already mapped to '%d'!".formatted(internalLabelKey, this.internalLabelDefinitions.get(internalLabelKey)));
        }
        this.internalLabelDefinitions.put(internalLabelKey, addressValue);
    }

    InternalLabelKey createInternalLabelKey(SourcePosition sourcePositionKey) {
        return new InternalLabelKey(this.tokenPollCounter, this.getHere(), sourcePositionKey);
    }

    void setHere(int value) {
        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException("Tried to set 'here' to a negative value or value greater than the 16-bit integer limit!");
        }
        this.here = value;
    }

    int addToHere(@Nullable Token token, int amount) throws OctoAssemblerException {
        int newHere = this.here + amount;
        if (newHere > 0xFFFF) {
            throw new OctoAssemblerException("ROM size exceeds the 16-bit integer limit!", token == null ? null : token.getSourcePosition());
        }
        return newHere;
    }

    void incrementHere(@Nullable Token token, int amount) throws OctoAssemblerException {
        this.here = this.addToHere(token, amount);
    }

    void incrementHere(@Nullable Token token, CodeElement codeElement) throws OctoAssemblerException {
        this.incrementHere(token, codeElement.getSizeInBytes());
    }

    void incrementHere(CodeElement codeElement) throws OctoAssemblerException {
        this.incrementHere(null, codeElement);
    }

    void pushIf(SourcePosition sourcePositionKey) {
        this.ifStack.push(this.createInternalLabelKey(sourcePositionKey));
    }

    Optional<InternalLabelKey> peekIf() {
        return this.ifStack.isEmpty() ? Optional.empty() : Optional.of(this.ifStack.peek());
    }

    Optional<InternalLabelKey> popIf() {
        return this.ifStack.isEmpty() ? Optional.empty() : Optional.of(this.ifStack.pop());
    }

    void pushLoop(SourcePosition sourcePositionKey) {
        this.loopStack.push(this.createInternalLabelKey(sourcePositionKey));
    }

    Optional<InternalLabelKey> peekLoop() {
        return this.loopStack.isEmpty() ? Optional.empty() : Optional.of(this.loopStack.peek());
    }

    Optional<InternalLabelKey> popLoop() {
        return this.loopStack.isEmpty() ? Optional.empty() : Optional.of(this.loopStack.pop());
    }

    Optional<Token> pollToken() {
        Optional<Token> token = this.tokenStream.poll();
        this.tokenPollCounter++;
        if (this.tokenPollCounter < 0) {
            throw new IllegalStateException("Exceeded maximum valid token poll counter value of '%d'!".formatted(Long.MAX_VALUE));
        }
        return token;
    }

    Token pollTokenOrThrow(String error, SourcePosition sourcePosition) throws OctoAssemblerException {
        return this.pollToken().orElseThrow(() -> new OctoAssemblerException(error, sourcePosition));
    }

    @SuppressWarnings("unchecked")
    <T extends Token> T pollTokenOrThrow(Class<T> tokenClass, String errorIfNotPresent, SourcePosition sourcePositionIfNotPresent, Function<Token, String> errorIfTokenNotInstance) throws OctoAssemblerException {
        Token token = this.pollTokenOrThrow(errorIfNotPresent, sourcePositionIfNotPresent);
        if (tokenClass.isInstance(token)) {
            return (T) token;
        } else {
            throw new OctoAssemblerException(errorIfTokenNotInstance.apply(token), token.getSourcePosition());
        }
    }

    Optional<Token> peekToken() {
        return this.tokenStream.peek();
    }

    @SuppressWarnings("unchecked")
    <T extends Token> Optional<T> peekToken(Class<T> tokenClass) {
        return this.peekToken().map(token -> {
            if (tokenClass.isInstance(token)) {
                return (T) token;
            } else {
                return null;
            }
        });
    }

    @SuppressWarnings("SameParameterValue")
    <T extends Token> Optional<T> peekTokenAndPollIfPresent(Class<T> tokenClass) {
        return this.peekToken(tokenClass).map(token -> {
            this.pollToken();
            return token;
        });
    }

    Collection<CodeElement> checkReservedName(Token token) throws OctoAssemblerException {
        if (token instanceof ReservedNameToken || token.getLexeme().startsWith("OCTO_")) {
            throw new OctoAssemblerException("The name '%s' is reserved and cannot be used as a label or directive name!".formatted(token.getLexeme()), token.getSourcePosition());
        } else {
            return List.of(new CallStatement(this.getHere(), this.getConstantDirective(token).<AddressArgument>map(constantDirectiveDefinition -> new AddressArgument.Resolved(constantDirectiveDefinition.getInt32Value() & 0xFFF)).orElseGet(() -> new AddressArgument.NamedLabelReference(token))));
        }
    }

    // TODO: Check if we should truncate or check for bit width for the non-literal cases!

    RegisterLiteralToken pollAsRegister(String error, SourcePosition sourcePosition) throws OctoAssemblerException {
        return switch (this.pollTokenOrThrow(error, sourcePosition)) {
            case RegisterLiteralToken registerLiteralToken -> registerLiteralToken;
            case Token nameToken -> {
                Optional<NonConstantDirectiveDefinition> nonConstantDirectiveDefinition = this.getNonConstantDirective(nameToken);
                if (nonConstantDirectiveDefinition.isEmpty()) {
                    throw new OctoAssemblerException("Unresolved directive name '%s'!".formatted(nameToken.getLexeme()), nameToken.getSourcePosition());
                }
                if (nonConstantDirectiveDefinition.get() instanceof AliasDefinition aliasDefinition) {
                    yield new RegisterLiteralToken(aliasDefinition.getRegisterIndex(), nameToken.getLexeme(), nameToken.getSourcePosition());
                } else {
                    throw new OctoAssemblerException("Expected register argument, got '%s'!".formatted(nameToken.getLexeme()), nameToken.getSourcePosition());
                }
            }
        };
    }

    IntegerLiteralToken pollAs2BitValue(String error, SourcePosition sourcePosition) throws OctoAssemblerException {
        return switch (this.pollTokenOrThrow(error, sourcePosition)) {
            case IntegerLiteralToken integerLiteralToken -> {
                if (integerLiteralToken.isUnsigned2Bits()) {
                    yield integerLiteralToken;
                } else {
                    throw new OctoAssemblerException("The integer argument '%d' does not fit in 2 bits. Must be in the range [0, 3]!".formatted(integerLiteralToken.getValue()), integerLiteralToken.getSourcePosition());
                }
            }
            case Token nameToken -> {
                Optional<ConstantDirectiveDefinition> constantDirectiveDefinition = this.getConstantDirective(nameToken);
                if (constantDirectiveDefinition.isEmpty()) {
                    throw new OctoAssemblerException("Unresolved directive name '%s'!".formatted(nameToken.getLexeme()), nameToken.getSourcePosition());
                }
                yield new IntegerLiteralToken(constantDirectiveDefinition.get().getUInt8Value() & 0xF, nameToken.getLexeme(), nameToken.getSourcePosition());
            }
        };
    }

    IntegerLiteralToken pollAsNibbleValue(String error, SourcePosition sourcePosition) throws OctoAssemblerException {
        return switch (this.pollTokenOrThrow(error, sourcePosition)) {
            case IntegerLiteralToken integerLiteralToken -> {
                if (integerLiteralToken.isUnsigned4Bits()) {
                    yield integerLiteralToken;
                } else {
                    throw new OctoAssemblerException("The integer argument '%d' does not fit in 4 bits. Must be in the range [0, 15]!".formatted(integerLiteralToken.getValue()), integerLiteralToken.getSourcePosition());
                }
            }
            case Token nameToken -> {
                Optional<ConstantDirectiveDefinition> constantDirectiveDefinition = this.getConstantDirective(nameToken);
                if (constantDirectiveDefinition.isEmpty()) {
                    throw new OctoAssemblerException("Unresolved directive name '%s'!".formatted(nameToken.getLexeme()), nameToken.getSourcePosition());
                }
                yield new IntegerLiteralToken(constantDirectiveDefinition.get().getUInt8Value() & 0xF, nameToken.getLexeme(), nameToken.getSourcePosition());
            }
        };
    }

    @SuppressWarnings("SameParameterValue")
    IntegerLiteralToken pollAsByteValue(String error, SourcePosition sourcePosition) throws OctoAssemblerException {
        return switch (this.pollTokenOrThrow(error, sourcePosition)) {
            case IntegerLiteralToken integerLiteralToken -> {
                if (integerLiteralToken.is8Bits()) {
                    yield integerLiteralToken;
                } else {
                    throw new OctoAssemblerException("The integer argument '%d' does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(integerLiteralToken.getValue()), integerLiteralToken.getSourcePosition());
                }
            }
            case Token nameToken -> {
                Optional<ConstantDirectiveDefinition> optionalConstantDirectiveDefinition = this.getConstantDirective(nameToken);
                if (optionalConstantDirectiveDefinition.isEmpty()) {
                    throw new OctoAssemblerException("Unresolved directive name '%s'!".formatted(nameToken.getLexeme()), nameToken.getSourcePosition());
                }
                ConstantDirectiveDefinition constantDirectiveDefinition = optionalConstantDirectiveDefinition.get();
                if (constantDirectiveDefinition.is8Bits()) {
                    yield new IntegerLiteralToken(constantDirectiveDefinition.getInt32Value(), nameToken.getLexeme(), nameToken.getSourcePosition());
                } else {
                    throw new OctoAssemblerException("The integer argument '%d' does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(constantDirectiveDefinition.getInt32Value()), nameToken.getSourcePosition());
                }

            }
        };
    }

    Token pollAsByteValueOrRegister(String error, SourcePosition sourcePosition) throws OctoAssemblerException {
        return switch (this.pollTokenOrThrow(error, sourcePosition)) {
            case RegisterLiteralToken registerLiteralToken -> registerLiteralToken;
            case IntegerLiteralToken integerLiteralToken -> {
                if (integerLiteralToken.is8Bits()) {
                    yield integerLiteralToken;
                } else {
                    throw new OctoAssemblerException("The integer argument '%d' does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(integerLiteralToken.getValue()), integerLiteralToken.getSourcePosition());
                }
            }
            case Token nameToken -> {
                Optional<NonConstantDirectiveDefinition> nonConstantDirectiveDefinition = this.getNonConstantDirective(nameToken);
                if (nonConstantDirectiveDefinition.isPresent() && nonConstantDirectiveDefinition.get() instanceof AliasDefinition aliasDefinition) {
                    yield new RegisterLiteralToken(aliasDefinition.getRegisterIndex(), nameToken.getLexeme(), nameToken.getSourcePosition());
                }

                Optional<ConstantDirectiveDefinition> optionalConstantDirectiveDefinition = this.getConstantDirective(nameToken);
                if (optionalConstantDirectiveDefinition.isPresent()) {
                    ConstantDirectiveDefinition constantDirectiveDefinition = optionalConstantDirectiveDefinition.get();
                    if (constantDirectiveDefinition.is8Bits()) {
                        yield new IntegerLiteralToken(constantDirectiveDefinition.getInt32Value(), nameToken.getLexeme(), nameToken.getSourcePosition());
                    } else {
                        throw new OctoAssemblerException("The integer argument '%d' does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(constantDirectiveDefinition.getInt32Value()), nameToken.getSourcePosition());
                    }
                }

                if (nonConstantDirectiveDefinition.isPresent()) {
                    throw new OctoAssemblerException("The directive name '%s' does not correspond to a constant or alias definition!".formatted(nameToken.getLexeme()), nameToken.getSourcePosition());
                }

                yield nameToken;
            }
        };
    }

    Token pollAsAddress12(String error, SourcePosition sourcePosition) throws OctoAssemblerException {
        return switch (this.pollTokenOrThrow(error, sourcePosition)) {
            case IntegerLiteralToken integerLiteralToken -> {
                if (integerLiteralToken.isUnsigned12Bits()) {
                    yield integerLiteralToken;
                } else {
                    throw new OctoAssemblerException("The integer argument '%d' does not fit in 12 bits!".formatted(integerLiteralToken.getValue()), integerLiteralToken.getSourcePosition());
                }
            }
            case Token nameToken -> {
                Optional<ConstantDirectiveDefinition> optionalConstantDirectiveDefinition = this.getConstantDirective(nameToken);
                if (optionalConstantDirectiveDefinition.isEmpty()) {
                    yield nameToken;
                } else {
                    ConstantDirectiveDefinition constantDirectiveDefinition = optionalConstantDirectiveDefinition.get();
                    if (constantDirectiveDefinition.isUnsigned12Bits()) {
                        yield new IntegerLiteralToken(constantDirectiveDefinition.getInt32Value(), nameToken.getLexeme(), nameToken.getSourcePosition());
                    } else {
                        throw new OctoAssemblerException("The integer argument '%d' does not fit in 12 bits!".formatted(constantDirectiveDefinition.getInt32Value()), nameToken.getSourcePosition());
                    }
                }
            }
        };
    }

    Token pollAsAddress16(String error, SourcePosition sourcePosition) throws OctoAssemblerException {
        return switch (this.pollTokenOrThrow(error, sourcePosition)) {
            case IntegerLiteralToken integerLiteralToken -> {
                if (integerLiteralToken.isUnsigned16Bits()) {
                    yield integerLiteralToken;
                } else {
                    throw new OctoAssemblerException("The integer argument '%d' does not fit in 16 bits!".formatted(integerLiteralToken.getValue()), integerLiteralToken.getSourcePosition());
                }
            }
            case Token nameToken -> {
                Optional<ConstantDirectiveDefinition> constantDirectiveDefinition = this.getConstantDirective(nameToken);
                if (constantDirectiveDefinition.isEmpty()) {
                    yield nameToken;
                } else {
                    yield new IntegerLiteralToken(constantDirectiveDefinition.get().getInt32Value() & 0xFFFF, nameToken.getLexeme(), nameToken.getSourcePosition());
                }
            }
        };
    }

    Optional<Token> checkTopLevelExpansion(Token token) throws OctoAssemblerException {
        if (token instanceof IdentifierToken identifierToken) {
            Optional<NonConstantDirectiveDefinition> nonConstantDirectiveDefinition = this.getNonConstantDirective(identifierToken);
            if (nonConstantDirectiveDefinition.isEmpty()) {
                return Optional.of(identifierToken);
            } else {
                switch (nonConstantDirectiveDefinition.get()) {
                    case AliasDefinition aliasDefinition -> {
                        return Optional.of(new RegisterLiteralToken(aliasDefinition.getRegisterIndex(), token.getLexeme(), token.getSourcePosition()));
                    }
                    case ExpandableDirectiveDefinition expandableDirectiveDefinition -> {
                        Token[] expansionArguments = new Token[expandableDirectiveDefinition.getExpansionArgumentQuantity()];
                        for (int i = 0; i < expansionArguments.length; i++) {
                            expansionArguments[i] = this.pollTokenOrThrow("Expected %d arguments following '%s' directive invocation!".formatted(expansionArguments.length, token.getLexeme()), token.getSourcePosition());
                        }
                        List<Token> expandedTokens = expandableDirectiveDefinition.expand(this.getHere(), token.getSourcePosition(), expansionArguments);
                        for (int i = expandedTokens.size() - 1; i >= 0; i--) {
                            this.tokenStream.offerFront(expandedTokens.get(i));
                        }
                        Optional<Token> front = this.pollToken();
                        if (front.isEmpty()) {
                            return Optional.empty();
                        } else {
                            return this.checkTopLevelExpansion(front.get());
                        }
                    }
                    default -> {
                        return Optional.of(identifierToken);
                    }
                }
            }
        } else {
            return Optional.of(token);
        }
    }

    byte getROMByte(int address) {
        address &= 0xFFFF;
        for (CodeElement codeElement : this.codeElements) {
            int codeElementOffset = codeElement.getOffset();
            if (address >= codeElementOffset && address < codeElementOffset + codeElement.getSizeInBytes()) {
                int offsetIntoOpcode = address - codeElementOffset;
                if (codeElement instanceof LabelableElement labelableElement) {
                    switch (labelableElement.getAddressArgument()) {
                        case AddressArgument.Resolved _ -> {}
                        case AddressArgument.Unresolved unresolved -> {
                            switch (unresolved) {
                                case AddressArgument.NamedLabelReference namedLabelReference -> this.getConstantDirective(namedLabelReference.token())
                                        .map(constantDirectiveDefinition -> {
                                            if (constantDirectiveDefinition instanceof LabelDefinition labelDefinition) {
                                                return labelDefinition;
                                            } else {
                                                return null;
                                            }
                                        }).ifPresent(labelDefinition -> labelableElement.resolve(labelDefinition.getAddress()));
                                case AddressArgument.InternalLabelReference internalLabelReference -> Optional.ofNullable(this.internalLabelDefinitions.get(internalLabelReference.internalLabelKey())).ifPresent(labelableElement::resolve);
                            }
                        }
                    }
                }
                return codeElement.getBytes().getBytes()[offsetIntoOpcode];
            }
        }
        return (byte) 0x00;
    }

    private enum OctoBuiltInConstants {
        OCTO_KEY_1("OCTO_KEY_1", 1),
        OCTO_KEY_2("OCTO_KEY_2", 2),
        OCTO_KEY_3("OCTO_KEY_3", 3),
        OCTO_KEY_4("OCTO_KEY_4", 12),
        OCTO_KEY_Q("OCTO_KEY_Q", 4),
        OCTO_KEY_W("OCTO_KEY_W", 5),
        OCTO_KEY_E("OCTO_KEY_E", 6),
        OCTO_KEY_R("OCTO_KEY_R", 13),
        OCTO_KEY_A("OCTO_KEY_A", 7),
        OCTO_KEY_S("OCTO_KEY_S", 8),
        OCTO_KEY_D("OCTO_KEY_D", 9),
        OCTO_KEY_F("OCTO_KEY_F", 14),
        OCTO_KEY_Z("OCTO_KEY_Z", 10),
        OCTO_KEY_X("OCTO_KEY_X", 0),
        OCTO_KEY_C("OCTO_KEY_C", 11),
        OCTO_KEY_V("OCTO_KEY_V", 15);

        private final String key;
        private final int value;

        OctoBuiltInConstants(String key, int value) {
            this.key = key;
            this.value = value;
        }

        private String getKey() {
            return this.key;
        }

        private int getValue() {
            return this.value;
        }

    }

}
