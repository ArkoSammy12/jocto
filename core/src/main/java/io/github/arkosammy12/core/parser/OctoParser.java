package io.github.arkosammy12.core.parser;

import io.github.arkosammy12.core.codegen.*;
import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.lexer.SourceStream;
import io.github.arkosammy12.core.parser.directive.DirectiveDefinition;
import io.github.arkosammy12.core.parser.directive.LabelDefinition;
import io.github.arkosammy12.core.token.*;

import java.util.*;

public class OctoParser {

    public ParserResult parseTokens(SourceStream<Token> tokenStream) {
        ParserContext parserContext = new ParserContext(tokenStream);
        try {
            this.parseTopLevel(parserContext);
            return new ParserResult.Ok(List.copyOf(parserContext.codeElements), parserContext.getLabelDefinitions());
        } catch (ParserException e) {
            return e.toErrorResult();
        }
    }

    private void parseTopLevel(ParserContext parserContext) throws ParserException {
        while (!parserContext.tokenStream.isEmpty()) {
            Optional<Token> optionalToken = parserContext.tokenStream.poll();
            if (optionalToken.isPresent()) {
                Token token = optionalToken.get();
                Optional<CodeElement> optionalCodeElement = this.parseToken(parserContext, token);
                if (optionalCodeElement.isPresent()) {
                    CodeElement codeElement = optionalCodeElement.get();
                    parserContext.codeElements.add(codeElement);
                    parserContext.incrementHere(token, codeElement);
                }
            }
        }
    }

    private Optional<CodeElement> parseIfBlockKeywordToken(ParserContext parserContext, IfBlockKeywordToken ifBlockKeywordToken) {
        return Optional.empty();
    }

    private Optional<CodeElement> parseLoopBlockKeywordToken(ParserContext parserContext, LoopBlockKeywordToken loopBlockKeywordToken) {
        return Optional.empty();
    }

    private Optional<CodeElement> parseToken(ParserContext parserContext, Token token) throws ParserException  {
        return switch (token) {
            case DirectiveToken directiveToken -> this.parseDirective(parserContext, directiveToken);
            case InstructionStatementNameToken instructionStatementNameToken -> this.parseInstructionStatementNameToken(parserContext, instructionStatementNameToken);
            case LiteralToken literalToken -> this.parseLiteral(parserContext, literalToken);
            case IndexRegisterToken indexRegisterToken -> this.parseIndexRegister(parserContext, indexRegisterToken);
            case AssignmentKeywordToken assignmentKeywordToken -> this.parseAssignmentKeywordToken(parserContext, assignmentKeywordToken);
            case IfBlockKeywordToken ifBlockKeywordToken -> this.parseIfBlockKeywordToken(parserContext, ifBlockKeywordToken);
            case LoopBlockKeywordToken loopBlockKeywordToken -> this.parseLoopBlockKeywordToken(parserContext, loopBlockKeywordToken);
            default -> this.checkReservedName(parserContext, token);
        };
    }

    private Optional<CodeElement> parseInstructionStatementNameToken(ParserContext parserContext, InstructionStatementNameToken instructionStatementNameToken) throws ParserException {
        return switch (instructionStatementNameToken) {
            case SemicolonToken _ -> Optional.of(new ReturnStatement(parserContext.here));
            case NonSymbolInstructionStatementKeywordToken nonSymbolInstructionStatementKeywordToken -> switch (nonSymbolInstructionStatementKeywordToken.getInstructionStatementKeyword()) {
                case RETURN -> Optional.of(new ReturnStatement(parserContext.here));
                case CLEAR -> Optional.of(new ClearStatement(parserContext.here));
                case BCD -> Optional.of(new BCDStatement(parserContext.here, this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected register literal after bcd statement!", instructionStatementNameToken.getSourcePosition()).getRegisterIndex()));
                case SAVE -> {
                    RegisterLiteralToken vx = this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected register literal after save statement!", instructionStatementNameToken.getSourcePosition());
                    Optional<DashToken> optionalDashToken = this.peekTokenAndPollIfPresent(DashToken.class, parserContext);
                    if (optionalDashToken.isPresent()) {
                        yield Optional.of(new SaveRegistersStatement(parserContext.here, vx.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected register literal after '-' in save instruction!", instructionStatementNameToken.getSourcePosition()).getRegisterIndex()));
                    } else {
                        yield Optional.of(new SaveRegistersStatement(parserContext.here, vx.getRegisterIndex()));
                    }
                }
                case LOAD -> {
                    RegisterLiteralToken vx = this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected register literal after save instruction", instructionStatementNameToken.getSourcePosition());
                    Optional<DashToken> optionalDashToken = this.peekTokenAndPollIfPresent(DashToken.class, parserContext);
                    if (optionalDashToken.isPresent()) {
                        yield Optional.of(new LoadRegistersStatement(parserContext.here, vx.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected register literal after '-' in save instruction!", instructionStatementNameToken.getSourcePosition()).getRegisterIndex()));
                    } else {
                        yield Optional.of(new LoadRegistersStatement(parserContext.here, vx.getRegisterIndex()));
                    }
                }
                case SPRITE -> {
                    RegisterLiteralToken vx = this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected 'vx' argument after sprite statement!", instructionStatementNameToken.getSourcePosition());
                    RegisterLiteralToken vy = this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected 'vy' argument after 'vx' argument in sprite statement!", instructionStatementNameToken.getSourcePosition());
                    IntegerLiteralToken n = this.pollTokenOrThrow(IntegerLiteralToken.class, parserContext, "Expected integer literal after 'vy' argument in sprite statement!", instructionStatementNameToken.getSourcePosition());
                    if (!n.isUnsigned4Bits()) {
                        throw new ParserException("Sprite statement argument %d for 'n' does not fit is not in the range [0, 15]".formatted(n.getValue()), instructionStatementNameToken.getSourcePosition());
                    }
                    yield Optional.of(new SpriteStatement(parserContext.here, vx.getRegisterIndex(), vy.getRegisterIndex(), n.getValue()));
                }
                case JUMP -> {
                    Token token = this.pollTokenOrThrow(parserContext, "Expected integer or label argument after jump statement!", instructionStatementNameToken.getSourcePosition());
                    if (token instanceof IntegerLiteralToken n) {
                        if (!n.isUnsigned12Bits()) {
                            throw new ParserException("Jump statement target %d does not fit in 12 bits!".formatted(n.getValue()), instructionStatementNameToken.getSourcePosition());
                        }
                        yield Optional.of(new JumpStatement(parserContext.here, new AddressArgument.Value(n.getValue())));
                    } else {
                        yield Optional.of(new JumpStatement(parserContext.here, new AddressArgument.LabelReference(token.getLexeme())));
                    }
                }
                case JUMP0 -> {
                    Token token = this.pollTokenOrThrow(parserContext, "Expected integer or label argument after jump0 statement!", instructionStatementNameToken.getSourcePosition());
                    if (token instanceof IntegerLiteralToken n) {
                        if (!n.isUnsigned12Bits()) {
                            throw new ParserException("Jump0 statement target %d does not fit in 12 bits!".formatted(n.getValue()), instructionStatementNameToken.getSourcePosition());
                        }
                        yield Optional.of(new JumpZeroStatement(parserContext.here, new AddressArgument.Value(n.getValue())));
                    } else {
                        yield Optional.of(new JumpZeroStatement(parserContext.here, new AddressArgument.LabelReference(token.getLexeme())));
                    }
                }
                case HIRES -> Optional.of(new HiresStatement(parserContext.here));
                case LORES -> Optional.of(new LoresStatement(parserContext.here));
                case SCROLL_DOWN -> {
                    IntegerLiteralToken n = this.pollTokenOrThrow(IntegerLiteralToken.class, parserContext, "Expected integer literal after 'vy' argument in scroll-down statement!", instructionStatementNameToken.getSourcePosition());
                    if (!n.isUnsigned4Bits()) {
                        throw new ParserException("Scroll down statement argument %d for 'n' does not fit is not in the range [0, 15]".formatted(n.getValue()), instructionStatementNameToken.getSourcePosition());
                    }
                    yield Optional.of(new ScrollDownStatement(parserContext.here, n.getValue()));
                }
                case SCROLL_LEFT -> Optional.of(new ScrollLeftStatement(parserContext.here));
                case SCROLL_RIGHT -> Optional.of(new ScrollRightStatement(parserContext.here));
                case EXIT -> Optional.of(new ExitStatement(parserContext.here));
                case SAVE_FLAGS -> Optional.of(new SaveFlagsStatement(parserContext.here, this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected 'vx' argument after saveflags statement!", instructionStatementNameToken.getSourcePosition()).getRegisterIndex()));
                case LOAD_FLAGS -> Optional.of(new LoadFlagsStatement(parserContext.here, this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected 'vx' argument after loadflags statement!", instructionStatementNameToken.getSourcePosition()).getRegisterIndex()));
                case PLANE -> {
                    IntegerLiteralToken n = this.pollTokenOrThrow(IntegerLiteralToken.class, parserContext, "Expected integer literal after 'vy' argument in plane statement!", instructionStatementNameToken.getSourcePosition());
                    if (!n.isUnsigned4Bits()) {
                        throw new ParserException("Plane statement argument %d for 'n' does not fit is not in the range [0, 15]".formatted(n.getValue()), instructionStatementNameToken.getSourcePosition());
                    }
                    yield Optional.of(new PlaneStatement(parserContext.here, n.getValue()));
                }
                case AUDIO -> Optional.of(new AudioStatement(parserContext.here));
                case PITCH -> {
                    AssignmentOperatorToken assignmentOperatorToken = this.pollTokenOrThrow(AssignmentOperatorToken.class, parserContext, "Expected assignment operator ':=' after pitch statement!", instructionStatementNameToken.getSourcePosition());
                    if (assignmentOperatorToken.getAssignmentOperation() == AssignmentOperation.SET) {
                        yield Optional.of(new SetPitchAssignment(parserContext.here, this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected register literal as pitch statement argument!", instructionStatementNameToken.getSourcePosition()).getRegisterIndex()));
                    } else {
                        throw new ParserException("Unexpected operator '%s' after pitch statement".formatted(assignmentOperatorToken.getLexeme()), instructionStatementNameToken.getSourcePosition());
                    }
                }
                case SCROLL_UP -> {
                    IntegerLiteralToken n = this.pollTokenOrThrow(IntegerLiteralToken.class, parserContext, "Expected integer literal after 'vy' argument in scroll-up statement!", instructionStatementNameToken.getSourcePosition());
                    if (!n.isUnsigned4Bits()) {
                        throw new ParserException("Scroll up statement argument %d for 'n' does not fit is not in the range [0, 15]".formatted(n.getValue()), instructionStatementNameToken.getSourcePosition());
                    }
                    yield Optional.of(new ScrollUpStatement(parserContext.here, n.getValue()));
                }
            };
        };
    }
    
    private Optional<CodeElement> parseLiteral(ParserContext parserContext, LiteralToken literalToken) throws ParserException {
        return switch (literalToken) {
            case RegisterLiteralToken registerLiteralToken -> this.parseRegisterLiteral(parserContext, registerLiteralToken);
            case IntegerLiteralToken integerLiteralToken -> {
                if (!integerLiteralToken.is8Bits()) {
                    throw new ParserException("Raw integer literal '%d' does not fit in 8 bits [-128, 255]".formatted(integerLiteralToken.getValue()), integerLiteralToken.getSourcePosition());
                }
                yield Optional.of(new ByteLiteral(parserContext.here, integerLiteralToken.getValue()));
            }
            case FloatLiteralToken floatLiteralToken -> this.checkReservedName(parserContext, floatLiteralToken);
            case StringLiteralToken stringLiteralToken -> this.checkReservedName(parserContext, stringLiteralToken);
        };
    }

    private Optional<CodeElement> parseAssignmentKeywordToken(ParserContext parserContext, AssignmentKeywordToken assignmentKeywordToken) throws ParserException {
        return (switch (assignmentKeywordToken.getAssignmentKeyword()) {
            case DELAY -> {
                if (this.pollTokenOrThrow(AssignmentOperatorToken.class, parserContext, "Expected assignment operator ':=' after delay statement!", assignmentKeywordToken.getSourcePosition()).getAssignmentOperation() != AssignmentOperation.SET) {
                    throw new ParserException("Expected assignment operator ':=' after delay statement!", assignmentKeywordToken.getSourcePosition());
                }
                yield Optional.of(new SetDelayTimerAssignment(parserContext.here, this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected 'vx' argument after delay statement!", assignmentKeywordToken.getSourcePosition()).getRegisterIndex()));
            }
            case BUZZER -> {
                if (this.pollTokenOrThrow(AssignmentOperatorToken.class, parserContext, "Expected assignment operator ':=' after buzzer statement!", assignmentKeywordToken.getSourcePosition()).getAssignmentOperation() != AssignmentOperation.SET) {
                    throw new ParserException("Expected assignment operator ':=' after buzzer statement!", assignmentKeywordToken.getSourcePosition());
                }
                yield Optional.of(new SetBuzzerTimerAssignment(parserContext.here, this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected 'vx' argument after delay statement!", assignmentKeywordToken.getSourcePosition()).getRegisterIndex()));
            }
            case PITCH -> {
                if (this.pollTokenOrThrow(AssignmentOperatorToken.class, parserContext, "Expected assignment operator ':=' after pitch statement!", assignmentKeywordToken.getSourcePosition()).getAssignmentOperation() != AssignmentOperation.SET) {
                    throw new ParserException("Expected assignment operator ':=' after pitch statement!", assignmentKeywordToken.getSourcePosition());
                }
                yield Optional.of(new SetPitchAssignment(parserContext.here, this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected 'vx' argument after delay statement!", assignmentKeywordToken.getSourcePosition()).getRegisterIndex()));
            }
            default -> this.checkReservedName(parserContext, assignmentKeywordToken);
        });
    }
    
    private Optional<CodeElement> parseRegisterLiteral(ParserContext parserContext, RegisterLiteralToken registerLiteralToken) throws ParserException {
        return switch (this.pollTokenOrThrow(AssignmentOperatorToken.class, parserContext, "Unknown operator on register assignment statement!", registerLiteralToken.getSourcePosition()).getAssignmentOperation()) {
            case BITWISE_OR -> Optional.of(new BitwiseOrRegisterAssignment(parserContext.here, registerLiteralToken.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected vy argument after 'vx |=' assignment!", registerLiteralToken.getSourcePosition()).getRegisterIndex()));
            case BITWISE_AND -> Optional.of(new BitwiseAndRegisterAssignment(parserContext.here, registerLiteralToken.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected vy argument after 'vx &=' assignment!", registerLiteralToken.getSourcePosition()).getRegisterIndex()));
            case BITWISE_XOR -> Optional.of(new BitwiseXorRegisterAssignment(parserContext.here, registerLiteralToken.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected vy argument after 'vx ^=' assignment!", registerLiteralToken.getSourcePosition()).getRegisterIndex()));
            case RIGHT_SHIFT -> Optional.of(new RightShiftRegisterAssignment(parserContext.here, registerLiteralToken.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected vy argument after 'vx >>=' assignment!", registerLiteralToken.getSourcePosition()).getRegisterIndex()));
            case LEFT_SHIFT -> Optional.of(new LeftShiftRegisterAssignment(parserContext.here, registerLiteralToken.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected vy argument after 'vx <<=' assignment!", registerLiteralToken.getSourcePosition()).getRegisterIndex()));
            case RIGHT_SUBTRACT -> Optional.of(new RightSubtractRegisterFromRegisterAssignment(parserContext.here, registerLiteralToken.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected vy argument after 'vx =-' assignment!", registerLiteralToken.getSourcePosition()).getRegisterIndex()));
            case ADD -> switch (this.pollTokenOrThrow(parserContext, "Unterminated 'vx += ' statement!", registerLiteralToken.getSourcePosition())) {
                case RegisterLiteralToken vy -> Optional.of(new AddRegisterToRegisterAssignment(parserContext.here, registerLiteralToken.getRegisterIndex(), vy.getRegisterIndex()));
                case IntegerLiteralToken n -> {
                    if (n.is8Bits()) {
                        yield Optional.of(new AddConstantToRegisterAssignment(parserContext.here, n.getValue()));
                    } else {
                        throw new ParserException("Argument '%d' for 'vx +=' does not fit in a byte!".formatted(n.getValue()), n.getSourcePosition());
                    }
                }
                case Token token -> throw new ParserException("Unexpected argument '%s' for 'vx +=' statement".formatted(token.getLexeme()), token.getSourcePosition());
            };
            case LEFT_SUBTRACT -> switch (this.pollTokenOrThrow(parserContext, "Unterminated 'vx -= ' statement!", registerLiteralToken.getSourcePosition())) {
                case RegisterLiteralToken vy -> Optional.of(new LeftSubtractRegisterFromRegisterAssignment(parserContext.here, registerLiteralToken.getRegisterIndex(), vy.getRegisterIndex()));
                case IntegerLiteralToken n -> {
                    if (n.is8Bits()) {
                        yield Optional.of(new AddConstantToRegisterAssignment(parserContext.here, -n.getValue()));
                    } else {
                        throw new ParserException("Argument '%d' for 'vx -=' does not fit in a byte!".formatted(n.getValue()), n.getSourcePosition());
                    }
                }
                case Token token -> throw new ParserException("Unexpected argument '%s' for 'vx -=' statement".formatted(token.getLexeme()), token.getSourcePosition());
            };
            case SET -> switch (this.pollTokenOrThrow(parserContext, "Unterminated 'vx := ' statement!", registerLiteralToken.getSourcePosition())) {
                case RegisterLiteralToken vy -> Optional.of(new SetRegisterToRegisterAssignment(parserContext.here, registerLiteralToken.getRegisterIndex(), vy.getRegisterIndex()));
                case IntegerLiteralToken n -> {
                    if (n.is8Bits()) {
                        yield Optional.of(new SetRegisterToConstantAssignment(parserContext.here, n.getValue()));
                    } else {
                        throw new ParserException("Argument '%d' for 'vx :=' does not fit in a byte!".formatted(n.getValue()), n.getSourcePosition());
                    }
                }
                case AssignmentKeywordToken assignmentKeywordToken -> switch (assignmentKeywordToken.getAssignmentKeyword()) {
                    case DELAY -> Optional.of(new SetRegisterToDelayTimerAssignment(parserContext.here));
                    case KEY -> Optional.of(new SetRegisterToKeyAssignment(parserContext.here));
                    case RANDOM -> {
                        IntegerLiteralToken nn = this.pollTokenOrThrow(IntegerLiteralToken.class, parserContext, "Expected literal argument after 'vx := random' assignment!", registerLiteralToken.getSourcePosition());
                        if (nn.is8Bits()) {
                            yield Optional.of(new SetRegisterToRandomAssignment(parserContext.here, nn.getValue()));
                        } else {
                            throw new ParserException("Argument '%d' for 'vx := random' does not fit in a byte!".formatted(nn.getValue()), nn.getSourcePosition());
                        }
                    }
                    default -> this.checkReservedName(parserContext, assignmentKeywordToken);
                };
                case Token token -> throw new ParserException("Unexpected argument '%s' for 'vx :=' statement".formatted(token.getLexeme()), token.getSourcePosition());
            };
        };
    }

    private Optional<CodeElement> parseIndexRegister(ParserContext parserContext, IndexRegisterToken indexRegisterToken) throws ParserException {
        return switch (this.pollTokenOrThrow(AssignmentOperatorToken.class, parserContext, "Expected assignment operators ':=' or '+=' after an 'i' assignment!", indexRegisterToken.getSourcePosition()).getAssignmentOperation()) {
            case ADD -> Optional.of(new IncrementIndexRegisterAssignment(parserContext.here, this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected 'vx' argument after 'i' increment statement!", indexRegisterToken.getSourcePosition()).getRegisterIndex()));
            case SET -> switch (this.pollTokenOrThrow(parserContext, "Expected assignment operators ':=' or '+=' after an 'i' assignment!", indexRegisterToken.getSourcePosition())) {
                case IntegerLiteralToken n -> {
                    if (!n.isUnsigned12Bits()) {
                        throw new ParserException("Argument '%d' for 'i' assignment does not fit in 12 bits!".formatted(n.getValue()), indexRegisterToken.getSourcePosition());
                    }
                    yield Optional.of(new SetIndexRegisterToConstantAssignment(parserContext.here, new AddressArgument.Value(n.getValue())));
                }
                case AssignmentKeywordToken assignmentKeywordToken -> switch (assignmentKeywordToken.getAssignmentKeyword()) {
                    case HEX -> Optional.of(new SetIndexRegisterToHexCharAssignment(parserContext.here, this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected 'vx' argument after 'i := hex' increment statement!", indexRegisterToken.getSourcePosition()).getRegisterIndex()));
                    case BIGHEX -> Optional.of(new SetIndexRegisterToBigHexCharAssignment(parserContext.here, this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected 'vx' argument after 'i := bighex' increment statement!", indexRegisterToken.getSourcePosition()).getRegisterIndex()));
                    case LONG -> {
                        Token longAssignmentArgumentToken = this.pollTokenOrThrow(parserContext, "Expected 12 bit integer or label argument for 'i := long' statement!", indexRegisterToken.getSourcePosition());
                        if (longAssignmentArgumentToken instanceof IntegerLiteralToken nnnn) {
                            if (!nnnn.isUnsigned16Bits()) {
                                throw new ParserException("Argument '%d' for 'i := long' assignment does not fit in 16 bits!".formatted(nnnn.getValue()), nnnn.getSourcePosition());
                            }
                            yield Optional.of(new SetIndexRegisterToLongConstantAssignment(parserContext.here, new AddressArgument.Value(nnnn.getValue())));
                        } else {
                            this.checkReservedName(parserContext, longAssignmentArgumentToken);
                            yield Optional.of(new SetIndexRegisterToLongConstantAssignment(parserContext.here, new AddressArgument.LabelReference(longAssignmentArgumentToken.getLexeme())));
                        }
                    }
                    default -> throw new ParserException("Unexpected name '%s' after 'i' assignment statement!".formatted(indexRegisterToken.getLexeme()), indexRegisterToken.getSourcePosition());
                };
                case Token labelToken -> {
                    this.checkReservedName(parserContext, labelToken);
                    yield Optional.of(new SetIndexRegisterToConstantAssignment(parserContext.here, new AddressArgument.LabelReference(labelToken.getLexeme())));
                }
            };
            default -> throw new ParserException("Unexpected operator '%s' after an 'i' assignment!".formatted(indexRegisterToken.getLexeme()), indexRegisterToken.getSourcePosition());
        };
    }

    private Optional<CodeElement> parseDirective(ParserContext parserContext, DirectiveToken directiveToken) throws ParserException {
        return switch (directiveToken.getDirective()) {
            case LABEL_DEFINITION -> {
                Token token = this.pollTokenOrThrow(parserContext, "Expected label name following ':' directive!", directiveToken.getSourcePosition());
                parserContext.addDirectiveDefinition(token, new LabelDefinition(token.getLexeme(), parserContext.here));
                yield Optional.empty();
            }
            case ORG -> Optional.empty();
            case BYTE -> Optional.empty();
            case CALC -> Optional.empty();
            case CALL -> Optional.empty();
            case NEXT -> Optional.empty();
            case ALIAS -> Optional.empty();
            case CONST -> Optional.empty();
            case MACRO -> Optional.empty();
            case PROTO -> Optional.empty();
            case ASSERT -> Optional.empty();
            case UNPACK -> Optional.empty();
            case MONITOR -> Optional.empty();
            case POINTER -> Optional.empty();
            case BREAKPOINT -> Optional.empty();
            case STRING_MODE -> Optional.empty();
        };
    }

    private Optional<Token> pollToken(ParserContext parserContext) {
        return parserContext.tokenStream.poll()
                .flatMap(token -> parserContext.getDirective(token)
                        .map(directiveDefinition -> {
                            this.expandDirective(parserContext, directiveDefinition);
                            return this.pollToken(parserContext);
                        }).orElse(Optional.of(token)));
    }

    private Token pollTokenOrThrow(ParserContext parserContext, String error, SourcePosition sourcePosition) throws ParserException {
        return this.pollToken(parserContext).orElseThrow(() -> new ParserException(error, sourcePosition));
    }

    @SuppressWarnings("unchecked")
    private <T extends Token> T pollTokenOrThrow(Class<T> tokenClass, ParserContext parserContext, String error, SourcePosition sourcePosition) throws ParserException {
        Token token = this.pollTokenOrThrow(parserContext, error, sourcePosition);
        if (tokenClass.isInstance(token)) {
            return (T) token;
        } else {
            throw new ParserException(error, sourcePosition);
        }
    }

    private Optional<Token> peekToken(ParserContext parserContext) {
        return parserContext.tokenStream.peek()
                .flatMap(token -> parserContext.getDirective(token)
                        .map(directiveDefinition -> {
                            this.expandDirective(parserContext, directiveDefinition);
                            return this.peekToken(parserContext);
                }).orElse(Optional.of(token)));
    }

    private void expandDirective(ParserContext parserContext, DirectiveDefinition directiveDefinition) {
        List<Token> directiveTokens = directiveDefinition.expand(parserContext.here);
        for (int i = directiveTokens.size() - 1; i >= 0; i--) {
            parserContext.tokenStream.offerFront(directiveTokens.get(i));
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Token> Optional<T> peekToken(Class<T> tokenClass, ParserContext parserContext) {
        return this.peekToken(parserContext).map(token -> {
            if (tokenClass.isInstance(token)) {
                return (T) token;
            } else {
                return null;
            }
        });
    }

    private <T extends Token> Optional<T> peekTokenAndPollIfPresent(Class<T> tokenClass, ParserContext parserContext) {
        return this.peekToken(tokenClass, parserContext).map(token -> {
            this.pollToken(parserContext);
            return token;
        });
    }

    private Optional<CodeElement> checkReservedName(ParserContext parserContext, Token token) throws ParserException {
        if (token instanceof ReservedNameToken) {
            throw new ParserException("The name '%s' is reserved and cannot be used as a label!".formatted(token.getLexeme()), token.getSourcePosition());
        } else {
            return Optional.of(new CallStatement(parserContext.here, new AddressArgument.LabelReference(token.getLexeme())));
        }
    }

    private class ParserContext {

        private final SourceStream<Token> tokenStream;
        private final List<CodeElement> codeElements = new ArrayList<>();
        private final Map<String, DirectiveDefinition> directiveDefinitions = new HashMap<>();
        private int here = 0x200;

        private ParserContext(SourceStream<Token> tokenStream) {
            this.tokenStream = tokenStream;
        }

        private Optional<DirectiveDefinition> getDirective(Token token) {
            return Optional.ofNullable(this.directiveDefinitions.get(token.getLexeme()));
        }

        private Map<String, LabelDefinition> getLabelDefinitions() {
            Map<String, LabelDefinition> labelDefinitions = new HashMap<>();
            for (Map.Entry<String, DirectiveDefinition> entries : directiveDefinitions.entrySet()) {
                if (entries.getValue() instanceof LabelDefinition labelDefinition) {
                    labelDefinitions.put(entries.getKey(), labelDefinition);
                }
            }
            return Map.copyOf(labelDefinitions);
        }

        private void addDirectiveDefinition(Token token, DirectiveDefinition directiveDefinition) throws ParserException {
            String name = token.getLexeme();
            if (this.directiveDefinitions.containsKey(name)) {
                throw new ParserException("Directive name '%s' is already defined!".formatted(name), token.getSourcePosition());
            }
            checkReservedName(this, token);
            this.directiveDefinitions.put(name, directiveDefinition);
        }

        private void incrementHere(Token token, CodeElement codeElement) throws ParserException {
            int newHere = this.here + codeElement.getSizeInBytes();
            if (newHere > 0xFFFF) {
                throw new ParserException("ROM size exceeds the 16-bit integer limit!", token.getSourcePosition());
            }
            this.here = newHere;
        }

    }

    private static class ParserException extends Exception {

        private final String error;
        private final SourcePosition sourcePosition;

        private ParserException(String error, SourcePosition sourcePosition) {
            this.error = error;
            this.sourcePosition = sourcePosition;
        }

        private ParserResult toErrorResult() {
            return new ParserResult.Error(this.error, this.sourcePosition);
        }

    }

}
