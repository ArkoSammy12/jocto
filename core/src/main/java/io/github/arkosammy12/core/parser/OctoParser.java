package io.github.arkosammy12.core.parser;

import io.github.arkosammy12.core.elements.*;
import io.github.arkosammy12.core.grammar.Delimiter;
import io.github.arkosammy12.core.parser.directive.*;
import io.github.arkosammy12.core.result.OctoAssemblerException;
import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.lexer.SourceStream;
import io.github.arkosammy12.core.result.OctoParserResult;
import io.github.arkosammy12.core.token.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

import static io.github.arkosammy12.core.token.FloatLiteralToken.toInt32JS;
import static io.github.arkosammy12.core.token.FloatLiteralToken.toUInt8JS;

public class OctoParser {

    private final int programStart;

    public OctoParser(int programStart) {
        if (programStart < 0) {
            throw new IllegalArgumentException("The program start value cannot be less than zero!");
        }
        this.programStart = programStart;
    }

    public OctoParserResult parseTokens(SourceStream<Token> tokenStream) {
        try {
            ParserContext parserContext = new ParserContext(tokenStream);

            // We start by assuming that we have to reserve the first two bytes for a jump to the 'main' label
            JumpStatement jumpToMainStatement = new JumpStatement(parserContext.getHere(), new AddressArgument.NamedLabelReference(new IdentifierToken("main", new SourcePosition(0, 0))));
            parserContext.incrementHere(jumpToMainStatement);
            parserContext.addCodeElement(jumpToMainStatement);

            while (!parserContext.getTokenStream().isEmpty()) {
                Optional<Token> token = parserContext.pollToken();
                if (token.isPresent()) {
                    Collection<? extends CodeElement> codeElements = switch (token.get()) {
                        case IfBlockKeywordToken ifBlockKeywordToken -> this.parseIfBlockKeywordToken(parserContext, ifBlockKeywordToken);
                        case LoopBlockKeywordToken loopBlockKeywordToken -> this.parseLoopBlockKeywordToken(parserContext, loopBlockKeywordToken);
                        case Token codeToken -> this.parseCodeToken(parserContext, codeToken);
                    };
                    for (CodeElement codeElement : codeElements) {
                        parserContext.addCodeElement(codeElement);
                    }
                }
            }

            if (!parserContext.foundMainLabel()) {
                return new OctoParserResult.Error("This program is missing a 'main' label!");
            }
            Optional<InternalLabelKey> ifKey = parserContext.peekIf();
            if (ifKey.isPresent()) {
                return new OctoParserResult.Error("This 'begin' does not have a matching end!", ifKey.get().sourcePositionKey());
            }
            Optional<InternalLabelKey> loopKey = parserContext.peekLoop();
            if (loopKey.isPresent()) {
                return new OctoParserResult.Error("This 'loop' does not have a matching 'again'!", loopKey.get().sourcePositionKey());
            }
            return new OctoParserResult.Ok(parserContext.getCodeElements(), parserContext.getLabelDefinitions(), parserContext.getInternalLabelDefinitions());
        } catch (OctoAssemblerException e) {
            return e.toParserResult();
        }
    }

    private Collection<? extends CodeElement> parseCodeToken(ParserContext parserContext, Token token) throws OctoAssemblerException {
        Collection<CodeElement> codeElements = this.parseToken(parserContext, token);
        for (CodeElement codeElement : codeElements) {
            parserContext.incrementHere(token, codeElement);
        }
        return codeElements;
    }

    private Collection<CodeElement> parseIfBlockKeywordToken(ParserContext parserContext, IfBlockKeywordToken ifBlockKeywordToken) throws OctoAssemblerException {
        return switch (ifBlockKeywordToken.getIfBlockKeywordLexeme()) {
            case IF -> {
                // HERE is pointing to the first opcode of the conditional expression
                Collection<CodeElement> ifBlockElements = new ArrayList<>(this.parseConditionalExpression(parserContext, ifBlockKeywordToken));
                IfBlockKeywordToken ifBlockBeginningToken = parserContext.pollTokenOrThrow(IfBlockKeywordToken.class, "Expected 'begin' or 'then' after if block conditional expression!", ifBlockKeywordToken.getSourcePosition(), token -> "Invalid if block beginning keyword '%s'!".formatted(token.getLexeme()));
                yield switch (ifBlockBeginningToken.getIfBlockKeywordLexeme()) {
                    case THEN -> {
                        // HERE is pointing to the skipped instruction, which is surrounded by the if-then block
                        List<CodeElement> ifThenBlockElements = new ArrayList<>();
                        while (ifThenBlockElements.isEmpty() && !parserContext.getTokenStream().isEmpty()) {
                            Optional<Token> ifThenBlockToken = parserContext.pollToken();
                            if (ifThenBlockToken.isPresent()) {
                                ifThenBlockElements.addAll(switch (ifThenBlockToken.get()) {
                                    case IfBlockKeywordToken innerIfBlockKeyword -> this.parseIfBlockKeywordToken(parserContext, innerIfBlockKeyword);
                                    case LoopBlockKeywordToken innerLoopBlockKeywordToken -> this.parseLoopBlockKeywordToken(parserContext, innerLoopBlockKeywordToken);
                                    case Token innerToken -> this.parseCodeToken(parserContext, innerToken);
                                });
                            }
                        }
                        ifBlockElements.addAll(ifThenBlockElements);
                        yield ifBlockElements;
                    }
                    case BEGIN -> {
                        // Invert the skip instruction since now the skip instruction will surround the jump instruction that
                        // prevents the if block from being executed, and not the execution of the if block itself
                        ifBlockElements = this.invertSkipConditions(ifBlockElements);

                        // HERE is pointing to the jump instruction that will jump to the end of the if-begin block or to the
                        // else case of the if-else block
                        JumpStatement jumpAboveIfBlockStatement = new JumpStatement(parserContext.getHere(), new AddressArgument.InternalLabelReference(new InternalLabelKey(parserContext.getHere(), ifBlockBeginningToken.getSourcePosition())));
                        parserContext.pushIf(ifBlockBeginningToken.getSourcePosition());
                        parserContext.incrementHere(ifBlockBeginningToken, jumpAboveIfBlockStatement);
                        ifBlockElements.add(jumpAboveIfBlockStatement);

                        // HERE is now pointing to the first instruction of the if block
                        yield ifBlockElements;
                    }
                    default -> throw new OctoAssemblerException("Illegal start of if block '%s'!".formatted(ifBlockBeginningToken.getLexeme()), ifBlockBeginningToken.getSourcePosition());
                };
            }
            case ELSE -> {
                Optional<InternalLabelKey> optionalIfKey = parserContext.popIf();
                if (optionalIfKey.isEmpty()) {
                    throw new OctoAssemblerException("This 'else' does not have a matching 'begin'!", ifBlockKeywordToken.getSourcePosition());
                } else {
                    InternalLabelKey ifKey = optionalIfKey.get();

                    // HERE is pointing to the jump instruction that jumps over the else block of an if-else block
                    JumpStatement jumpAboveElseBlockStatement = new JumpStatement(parserContext.getHere(), new AddressArgument.InternalLabelReference(new InternalLabelKey(parserContext.getHere(), ifBlockKeywordToken.getSourcePosition())));
                    parserContext.pushIf(ifBlockKeywordToken.getSourcePosition());
                    parserContext.incrementHere(ifBlockKeywordToken, jumpAboveElseBlockStatement);

                    // HERE is pointing to the first instruction of the else block
                    parserContext.addInternalLabelDefinition(ifKey, parserContext.getHere());
                    yield List.of(jumpAboveElseBlockStatement);
                }
            }
            case END -> {
                Optional<InternalLabelKey> ifKey = parserContext.popIf();
                if (ifKey.isEmpty()) {
                    throw new OctoAssemblerException("This 'else' does not have a matching 'begin'!", ifBlockKeywordToken.getSourcePosition());
                } else {
                    // HERE is pointing to the first instruction after the if-begin or if-else block, ending it
                    parserContext.addInternalLabelDefinition(ifKey.get(), parserContext.getHere());
                    yield List.of();
                }
            }
            case THEN -> throw new OctoAssemblerException("This 'then' does not have a matching 'if'!", ifBlockKeywordToken.getSourcePosition());
            case BEGIN -> throw new OctoAssemblerException("This 'begin' does not have a matching 'if'!", ifBlockKeywordToken.getSourcePosition());
        };
    }

    private Collection<CodeElement> parseLoopBlockKeywordToken(ParserContext parserContext, LoopBlockKeywordToken loopBlockKeywordToken) throws OctoAssemblerException {
        return switch (loopBlockKeywordToken.getLoopBlockKeyword()) {
            case LOOP -> {
                // HERE is pointing to the first instruction after the 'loop' keyword, starting the loop block
                parserContext.pushLoop(loopBlockKeywordToken.getSourcePosition());
                yield List.of();
            }
            case AGAIN -> {
                Optional<InternalLabelKey> optionalLoopKey = parserContext.popLoop();
                if (optionalLoopKey.isEmpty()) {
                    throw new OctoAssemblerException("This 'again' does not have a matching 'loop'!", loopBlockKeywordToken.getSourcePosition());
                } else {
                    // HERE is pointing to the jump instruction that jumps back to the start of the loop block, ending the loop block
                    InternalLabelKey loopKey = optionalLoopKey.get();
                    JumpStatement jumpToLoopBeginningStatement = new JumpStatement(parserContext.getHere(), new AddressArgument.Resolved(loopKey.addressKey()));
                    parserContext.incrementHere(loopBlockKeywordToken, jumpToLoopBeginningStatement);

                    // HERE is pointing to the first instruction after the loop block, ending it
                    parserContext.addInternalLabelDefinition(loopKey, parserContext.getHere());
                    yield List.of(jumpToLoopBeginningStatement);
                }
            }
            case WHILE -> {
                Optional<InternalLabelKey> loopKey = parserContext.peekLoop();
                if (loopKey.isEmpty()) {
                    throw new OctoAssemblerException("This 'while' is not within a 'loop'!", loopBlockKeywordToken.getSourcePosition());
                } else {
                    // HERE is pointing to first opcode of the conditional expression
                    Collection<CodeElement> whileStatementElements = this.invertSkipConditions(this.parseConditionalExpression(parserContext, loopBlockKeywordToken));

                    // HERE is pointing to the jump statement that will jump to after the loop block
                    JumpStatement jumpToLoopEndStatement = new JumpStatement(parserContext.getHere(), new AddressArgument.InternalLabelReference(loopKey.get()));
                    parserContext.incrementHere(loopBlockKeywordToken, jumpToLoopEndStatement);
                    whileStatementElements.add(jumpToLoopEndStatement);

                    // HERE now points to the first instruction after the while statement
                    yield whileStatementElements;
                }
            }
        };
    }

    private Collection<CodeElement> parseConditionalExpression(ParserContext parserContext, Token token) throws OctoAssemblerException {
        RegisterLiteralToken vx = parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected register operand in conditional expression!", token.getSourcePosition(), unmatchedToken -> "The operand '%s' is not a register!".formatted(unmatchedToken.getLexeme()));
        ConditionalOperatorToken conditionalOperatorToken = parserContext.pollTokenOrThrow(ConditionalOperatorToken.class, "Expected conditional operator in conditional statement!", vx.getSourcePosition(), unmatchedToken -> "Unknown conditional operator '%s'!".formatted(unmatchedToken.getLexeme()));
        ConditionalOperation conditionalOperation = conditionalOperatorToken.getConditionalOperation();
        return switch (conditionalOperation) {
            case KEY_PRESSED -> {
                CodeElement skipIfKeyNotPressed = new SkipIfKeyNotPressed(parserContext.getHere(), vx.getRegisterIndex());
                parserContext.incrementHere(conditionalOperatorToken, skipIfKeyNotPressed);
                yield List.of(skipIfKeyNotPressed);
            }
            case KEY_NOT_PRESSED -> {
                CodeElement skipIfKeyPressed = new SkipIfKeyPressed(parserContext.getHere(), vx.getRegisterIndex());
                parserContext.incrementHere(conditionalOperatorToken, skipIfKeyPressed);
                yield List.of(skipIfKeyPressed);
            }
            default -> {
                Token argumentToken = parserContext.pollTokenOrThrow("Expected register or byte argument in conditional expression!", conditionalOperatorToken.getSourcePosition());
                yield switch (argumentToken) {
                    case RegisterLiteralToken vy -> switch (conditionalOperation) {
                        case EQUALITY -> {
                            CodeElement skipIfRegistersNotEqual = new SkipIfRegistersNotEqual(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex());
                            parserContext.incrementHere(conditionalOperatorToken, skipIfRegistersNotEqual);
                            yield List.of(skipIfRegistersNotEqual);
                        }
                        case INEQUALITY -> {
                            CodeElement skipIfRegistersEqual = new SkipIfRegistersEqual(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex());
                            parserContext.incrementHere(conditionalOperatorToken, skipIfRegistersEqual);
                            yield List.of(skipIfRegistersEqual);
                        }
                        case GREATER_THAN -> {
                            CodeElement assign = new SetRegisterToRegisterAssignment(parserContext.getHere(), 0xF, vy.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, assign);
                            CodeElement subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.getHere(), 0xF, vx.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, subtract);
                            CodeElement skip = new SkipIfRegisterNotEqualsConstant(parserContext.getHere(), 0xF, 0x00);
                            parserContext.incrementHere(argumentToken, skip);
                            yield List.of(assign, subtract, skip);
                        }
                        case GREATER_THAN_OR_EQUALS -> {
                            CodeElement assign = new SetRegisterToRegisterAssignment(parserContext.getHere(), 0xF, vy.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, assign);
                            CodeElement subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.getHere(), 0xF, vx.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, subtract);
                            CodeElement skip = new SkipIfRegisterEqualsConstant(parserContext.getHere(), 0xF, 0x00);
                            parserContext.incrementHere(argumentToken, skip);
                            yield List.of(assign, subtract, skip);
                        }
                        case LESS_THAN -> {
                            CodeElement assign = new SetRegisterToRegisterAssignment(parserContext.getHere(), 0xF, vy.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, assign);
                            CodeElement subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.getHere(), 0xF, vx.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, subtract);
                            CodeElement skip = new SkipIfRegisterNotEqualsConstant(parserContext.getHere(), 0xF, 0x00);
                            parserContext.incrementHere(argumentToken, skip);
                            yield List.of(assign, subtract, skip);
                        }
                        case LESS_THAN_OR_EQUALS -> {
                            CodeElement assign = new SetRegisterToRegisterAssignment(parserContext.getHere(), 0xF, vy.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, assign);
                            CodeElement subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.getHere(), 0xF, vx.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, subtract);
                            CodeElement skip = new SkipIfRegisterEqualsConstant(parserContext.getHere(), 0xF, 0x00);
                            parserContext.incrementHere(argumentToken, skip);
                            yield List.of(assign, subtract, skip);
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + conditionalOperation);
                    };
                    case NumberLiteralToken nn -> {
                        if (!nn.is8Bits()) {
                            throw new OctoAssemblerException("Conditional operand '%d' does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(nn.getIntValue()), nn.getSourcePosition());
                        }
                        yield switch (conditionalOperation) {
                            case EQUALITY -> {
                                CodeElement skipIfRegisterNotEqualsConstant = new SkipIfRegisterNotEqualsConstant(parserContext.getHere(), vx.getRegisterIndex(), nn.getIntValue());
                                parserContext.incrementHere(conditionalOperatorToken, skipIfRegisterNotEqualsConstant);
                                yield List.of(skipIfRegisterNotEqualsConstant);
                            }
                            case INEQUALITY -> {
                                CodeElement skipIfRegisterEqualsConstant = new SkipIfRegisterEqualsConstant(parserContext.getHere(), vx.getRegisterIndex(), nn.getIntValue());
                                parserContext.incrementHere(conditionalOperatorToken, skipIfRegisterEqualsConstant);
                                yield List.of(skipIfRegisterEqualsConstant);
                            }
                            case GREATER_THAN -> {
                                CodeElement assign = new SetRegisterToConstantAssignment(parserContext.getHere(), 0xF, nn.getIntValue());
                                parserContext.incrementHere(argumentToken, assign);
                                CodeElement subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.getHere(), 0xF, vx.getRegisterIndex());
                                parserContext.incrementHere(argumentToken, subtract);
                                CodeElement skip = new SkipIfRegisterNotEqualsConstant(parserContext.getHere(), 0xF, 0x00);
                                parserContext.incrementHere(argumentToken, skip);
                                yield List.of(assign, subtract, skip);
                            }
                            case GREATER_THAN_OR_EQUALS -> {
                                CodeElement assign = new SetRegisterToConstantAssignment(parserContext.getHere(), 0xF, nn.getIntValue());
                                parserContext.incrementHere(argumentToken, assign);
                                CodeElement subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.getHere(), 0xF, vx.getRegisterIndex());
                                parserContext.incrementHere(argumentToken, subtract);
                                CodeElement skip = new SkipIfRegisterEqualsConstant(parserContext.getHere(), 0xF, 0x00);
                                parserContext.incrementHere(argumentToken, skip);
                                yield List.of(assign, subtract, skip);
                            }
                            case LESS_THAN -> {
                                CodeElement assign = new SetRegisterToConstantAssignment(parserContext.getHere(), 0xF, nn.getIntValue());
                                parserContext.incrementHere(argumentToken, assign);
                                CodeElement subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.getHere(), 0xF, vx.getRegisterIndex());
                                parserContext.incrementHere(argumentToken, subtract);
                                CodeElement skip = new SkipIfRegisterNotEqualsConstant(parserContext.getHere(), 0xF, 0x00);
                                parserContext.incrementHere(argumentToken, skip);
                                yield List.of(assign, subtract, skip);
                            }
                            case LESS_THAN_OR_EQUALS -> {
                                CodeElement assign = new SetRegisterToConstantAssignment(parserContext.getHere(), 0xF, nn.getIntValue());
                                parserContext.incrementHere(argumentToken, assign);
                                CodeElement subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.getHere(), 0xF, vx.getRegisterIndex());
                                parserContext.incrementHere(argumentToken, subtract);
                                CodeElement skip = new SkipIfRegisterEqualsConstant(parserContext.getHere(), 0xF, 0x00);
                                parserContext.incrementHere(argumentToken, skip);
                                yield List.of(assign, subtract, skip);
                            }
                            default -> throw new IllegalStateException("Unexpected value: " + conditionalOperation);
                        };
                    }
                    case Token t -> throw new OctoAssemblerException("Invalid conditional expression operand '%s'!".formatted(t.getLexeme()), t.getSourcePosition());
                };
            }
        };
    }

    private Collection<CodeElement> parseToken(ParserContext parserContext, Token token) throws OctoAssemblerException {
        return switch (token) {
            case DirectiveToken directiveToken -> this.parseDirective(parserContext, directiveToken);
            case InstructionStatementNameToken instructionStatementNameToken -> this.parseInstructionStatementNameToken(parserContext, instructionStatementNameToken);
            case LiteralToken literalToken -> this.parseLiteral(parserContext, literalToken);
            case IndexRegisterToken indexRegisterToken -> this.parseIndexRegister(parserContext, indexRegisterToken);
            case AssignmentKeywordToken assignmentKeywordToken -> this.parseAssignmentKeywordToken(parserContext, assignmentKeywordToken);
            default -> parserContext.checkReservedName(token);
        };
    }

    private Collection<CodeElement> parseInstructionStatementNameToken(ParserContext parserContext, InstructionStatementNameToken instructionStatementNameToken) throws OctoAssemblerException {
        return switch (instructionStatementNameToken) {
            case SemicolonToken _ -> List.of(new ReturnStatement(parserContext.getHere()));
            case NonSymbolInstructionStatementKeywordToken nonSymbolInstructionStatementKeywordToken -> switch (nonSymbolInstructionStatementKeywordToken.getInstructionStatementKeyword()) {
                case RETURN -> List.of(new ReturnStatement(parserContext.getHere()));
                case CLEAR -> List.of(new ClearScreenStatement(parserContext.getHere()));
                case BCD -> List.of(new BCDStatement(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected register operand in 'bcd' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                case SAVE -> {
                    RegisterLiteralToken vx = parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected register operand in 'save' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme()));
                    Optional<DashToken> dashToken = parserContext.peekTokenAndPollIfPresent(DashToken.class);
                    if (dashToken.isPresent()) {
                        yield List.of(new SaveRegistersStatement(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected register operand after '-' in 'save' statement!", dashToken.get().getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                    } else {
                        yield List.of(new SaveRegistersStatement(parserContext.getHere(), vx.getRegisterIndex()));
                    }
                }
                case LOAD -> {
                    RegisterLiteralToken vx = parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected register in 'load' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme()));
                    Optional<DashToken> dashToken = parserContext.peekTokenAndPollIfPresent(DashToken.class);
                    if (dashToken.isPresent()) {
                        yield List.of(new LoadRegistersStatement(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected register operand after '-' in 'load' statement!", dashToken.get().getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                    } else {
                        yield List.of(new LoadRegistersStatement(parserContext.getHere(), vx.getRegisterIndex()));
                    }
                }
                case SPRITE -> {
                    RegisterLiteralToken vx = parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' register operand in 'sprite' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme()));
                    RegisterLiteralToken vy = parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' register operand in 'sprite' statement!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme()));
                    NumberLiteralToken n = parserContext.pollTokenOrThrow(NumberLiteralToken.class, "Expected 'n' number operand in 'sprite' statement!", vy.getSourcePosition(), token -> "The operand '%s' is not an integer!".formatted(token.getLexeme()));
                    if (n.isUnsigned4Bits()) {
                        yield List.of(new SpriteStatement(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex(), n.getIntValue()));
                    } else {
                        throw new OctoAssemblerException("The 'sprite' statement operand '%d' for 'n' does not fit in 4 bits. Must be in the range [0, 15]!".formatted(n.getIntValue()), n.getSourcePosition());
                    }
                }
                case JUMP -> {
                    Token token = parserContext.pollTokenOrThrow("Expected number or label operand in 'jump' statement!", instructionStatementNameToken.getSourcePosition());
                    if (token instanceof NumberLiteralToken n) {
                        if (n.isUnsigned12Bits()) {
                            yield List.of(new JumpStatement(parserContext.getHere(), new AddressArgument.Resolved(n.getIntValue())));
                        } else {
                            throw new OctoAssemblerException("The 'jump' statement target '%d' does not fit in 12 bits!".formatted(n.getIntValue()), n.getSourcePosition());
                        }
                    } else {
                        yield List.of(new JumpStatement(parserContext.getHere(), new AddressArgument.NamedLabelReference(token)));
                    }
                }
                case JUMP0 -> {
                    Token token = parserContext.pollTokenOrThrow("Expected number or label operand in 'jump0' statement!", instructionStatementNameToken.getSourcePosition());
                    if (token instanceof NumberLiteralToken n) {
                        if (n.isUnsigned12Bits()) {
                            yield List.of(new JumpZeroStatement(parserContext.getHere(), new AddressArgument.Resolved(n.getIntValue())));
                        } else {
                            throw new OctoAssemblerException("The 'jump0' statement target '%d' does not fit in 12 bits!".formatted(n.getIntValue()), n.getSourcePosition());
                        }
                    } else {
                        yield List.of(new JumpZeroStatement(parserContext.getHere(), new AddressArgument.NamedLabelReference(token)));
                    }
                }
                case HIRES -> List.of(new HiresStatement(parserContext.getHere()));
                case LORES -> List.of(new LoresStatement(parserContext.getHere()));
                case SCROLL_DOWN -> {
                    NumberLiteralToken n = parserContext.pollTokenOrThrow(NumberLiteralToken.class, "Expected number operand in 'scroll-down' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The operand '%s' is not an integer!".formatted(token.getLexeme()));
                    if (n.isUnsigned4Bits()) {
                        yield List.of(new ScrollDownStatement(parserContext.getHere(), n.getIntValue()));
                    } else {
                        throw new OctoAssemblerException("The 'scroll-down' statement operand '%d' for 'n' does not fit in 4 bits. Must be in the range [0, 15]!".formatted(n.getIntValue()), n.getSourcePosition());
                    }
                }
                case SCROLL_LEFT -> List.of(new ScrollLeftStatement(parserContext.getHere()));
                case SCROLL_RIGHT -> List.of(new ScrollRightStatement(parserContext.getHere()));
                case EXIT -> List.of(new ExitStatement(parserContext.getHere()));
                case SAVE_FLAGS -> List.of(new SaveFlagsStatement(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' register operand in 'saveflags' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                case LOAD_FLAGS -> List.of(new LoadFlagsStatement(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' register operand in 'loadflags' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The argument '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                case PLANE -> {
                    NumberLiteralToken n = parserContext.pollTokenOrThrow(NumberLiteralToken.class, "Expected number operand in 'plane' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The operand '%s' is not an integer!".formatted(token.getLexeme()));
                    if (n.isUnsigned4Bits()) {
                        yield List.of(new SetBitplanesStatement(parserContext.getHere(), n.getIntValue()));
                    } else {
                        throw new OctoAssemblerException("The 'plane' statement operand %d for 'n' does not fit in 4 bits. Must be in the range [0, 15]!".formatted(n.getIntValue()), n.getSourcePosition());
                    }
                }
                case AUDIO -> List.of(new AudioStatement(parserContext.getHere()));
                case PITCH -> {
                    AssignmentOperatorToken assignmentOperatorToken = parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operator ':=' in 'pitch' assignment!", instructionStatementNameToken.getSourcePosition(), token -> "Unknown assignment operator '%s' in 'pitch' statement!".formatted(token.getLexeme()));
                    if (assignmentOperatorToken.getAssignmentOperation() == AssignmentOperation.SET) {
                        yield List.of(new SetPitchAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected register operand in 'pitch' statement!", assignmentOperatorToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                    } else {
                        throw new OctoAssemblerException("Unknown assignment operator '%s' after 'pitch' statement".formatted(assignmentOperatorToken.getLexeme()), assignmentOperatorToken.getSourcePosition());
                    }
                }
                case SCROLL_UP -> {
                    NumberLiteralToken n = parserContext.pollTokenOrThrow(NumberLiteralToken.class, "Expected number operand in 'scroll-up' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The operand '%s' is not a number!".formatted(token.getLexeme()));
                    if (n.isUnsigned4Bits()) {
                        yield List.of(new ScrollUpStatement(parserContext.getHere(), n.getIntValue()));
                    } else {
                        throw new OctoAssemblerException("The 'scroll-up' statement operand '%d' for 'n' does not fit in 4 bits. Must be in the range [0, 15]!".formatted(n.getIntValue()), n.getSourcePosition());
                    }
                }
            };
        };
    }

    private Collection<CodeElement> parseLiteral(ParserContext parserContext, LiteralToken literalToken) throws OctoAssemblerException {
        return switch (literalToken) {
            case RegisterLiteralToken registerLiteralToken -> this.parseRegisterLiteral(parserContext, registerLiteralToken);
            case NumberLiteralToken numberLiteralToken -> {
                // Account for the possibility that this number literal token came from a :const or :calc definition
                Optional<DirectiveDefinition> optionalDirectiveDefinition = parserContext.getDirective(numberLiteralToken);
                if (optionalDirectiveDefinition.isPresent()) {
                    DirectiveDefinition directiveDefinition = optionalDirectiveDefinition.get();
                    if ((directiveDefinition instanceof ConstDefinition) || (directiveDefinition instanceof CalcDefinition)) {
                        if (numberLiteralToken.isUnsigned12Bits()) {
                            parserContext.checkReservedName(numberLiteralToken);
                            yield List.of(new CallStatement(parserContext.getHere(), new AddressArgument.Resolved(numberLiteralToken.getIntValue())));
                        } else {
                            throw new OctoAssemblerException("Argument '%d' does not fit in 12 bits!".formatted(numberLiteralToken.getIntValue()), numberLiteralToken.getSourcePosition());
                        }
                    }
                }
                yield switch (numberLiteralToken) {
                    case IntegerLiteralToken integerLiteralToken -> {
                        if (integerLiteralToken.is8Bits()) {
                            yield List.of(new ByteElement(parserContext.getHere(), integerLiteralToken.getIntValue()));
                        } else {
                            throw new OctoAssemblerException("Raw integer literal '%d' does not fit in 8 bits [-128, 255]".formatted(integerLiteralToken.getIntValue()), integerLiteralToken.getSourcePosition());
                        }
                    }
                    case FloatLiteralToken floatLiteralToken -> parserContext.checkReservedName(floatLiteralToken);
                };
            }
            case StringLiteralToken stringLiteralToken -> parserContext.checkReservedName(stringLiteralToken);
        };
    }

    private Collection<CodeElement> parseAssignmentKeywordToken(ParserContext parserContext, AssignmentKeywordToken assignmentKeywordToken) throws OctoAssemblerException {
        return switch (assignmentKeywordToken.getAssignmentKeyword()) {
            case DELAY -> {
                if (parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operator ':=' after 'delay' assignment!", assignmentKeywordToken.getSourcePosition(), token -> "Unknown assignment operator '%s'!".formatted(token.getLexeme())).getAssignmentOperation() == AssignmentOperation.SET) {
                    yield List.of(new SetDelayTimerAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' register operand after delay statement!", assignmentKeywordToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                } else {
                    throw new OctoAssemblerException("Expected assignment operator ':=' after 'delay' statement!", assignmentKeywordToken.getSourcePosition());
                }
            }
            case BUZZER -> {
                if (parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operator ':=' after 'buzzer' assignment!", assignmentKeywordToken.getSourcePosition(), token -> "Unknown assignment operator '%s'!".formatted(token.getLexeme())).getAssignmentOperation() == AssignmentOperation.SET) {
                    yield List.of(new SetSoundTimerAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' register operand after delay statement!", assignmentKeywordToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                } else {
                    throw new OctoAssemblerException("Expected assignment operator ':=' after 'buzzer' statement!", assignmentKeywordToken.getSourcePosition());
                }
            }
            case PITCH -> {
                if (parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operator ':=' after 'pitch' assignment!", assignmentKeywordToken.getSourcePosition(), token -> "Unknown assignment operator '%s'!".formatted(token.getLexeme())).getAssignmentOperation() == AssignmentOperation.SET) {
                    yield List.of(new SetPitchAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' register operand after delay statement!", assignmentKeywordToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                } else {
                    throw new OctoAssemblerException("Expected assignment operator ':=' after 'pitch' statement!", assignmentKeywordToken.getSourcePosition());
                }
            }
            default -> parserContext.checkReservedName(assignmentKeywordToken);
        };
    }

    private Collection<CodeElement> parseRegisterLiteral(ParserContext parserContext, RegisterLiteralToken vx) throws OctoAssemblerException {
        return switch (parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operator after register assignment!", vx.getSourcePosition(), token -> "Unknown register assignment operator '%s'!".formatted(token.getLexeme())).getAssignmentOperation()) {
            case BITWISE_OR -> List.of(new BitwiseORRegistersAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' register operand after 'vx |=' assignment!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case BITWISE_AND -> List.of(new BitwiseANDRegistersAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' register operand after 'vx &=' assignment!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case BITWISE_XOR -> List.of(new BitwiseXORRegistersAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' register operand after 'vx ^=' assignment!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case RIGHT_SHIFT -> List.of(new RightShiftRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' register operand after 'vx >>=' assignment!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case LEFT_SHIFT -> List.of(new LeftShiftRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' register operand after 'vx <<=' assignment!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case RIGHT_SUBTRACT -> List.of(new RightSubtractRegisterFromRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' register operand after 'vx =-' assignment!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case ADD -> switch (parserContext.pollTokenOrThrow("Expected operand after 'vx +=' statement!", vx.getSourcePosition())) {
                case RegisterLiteralToken vy -> List.of(new AddRegisterToRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex()));
                case NumberLiteralToken nn -> {
                    if (nn.is8Bits()) {
                        yield List.of(new AddConstantToRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), nn.getIntValue()));
                    } else {
                        throw new OctoAssemblerException("Operand '%d' for 'vx +=' assignment does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(nn.getIntValue()), nn.getSourcePosition());
                    }
                }
                case Token token -> throw new OctoAssemblerException("Operand '%s' for 'vx +=' statement is not a register or number!".formatted(token.getLexeme()), token.getSourcePosition());
            };
            case LEFT_SUBTRACT -> switch (parserContext.pollTokenOrThrow("Expected operand after 'vx -=' statement!", vx.getSourcePosition())) {
                case RegisterLiteralToken vy -> List.of(new LeftSubtractRegisterFromRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex()));
                case NumberLiteralToken nn -> {
                    if (nn.is8Bits()) {
                        yield List.of(new AddConstantToRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), -nn.getIntValue()));
                    } else {
                        throw new OctoAssemblerException("Operand '%d' for 'vx -=' assignment does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(nn.getIntValue()), nn.getSourcePosition());
                    }
                }
                case Token token -> throw new OctoAssemblerException("Operand '%s' for 'vx -=' statement is not a register or a number!".formatted(token.getLexeme()), token.getSourcePosition());
            };
            case SET -> switch (parserContext.pollTokenOrThrow("Expected register or number operand after 'vx :=' statement!", vx.getSourcePosition())) {
                case RegisterLiteralToken vy -> List.of(new SetRegisterToRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex()));
                case NumberLiteralToken nn -> {
                    if (nn.is8Bits()) {
                        yield List.of(new SetRegisterToConstantAssignment(parserContext.getHere(), vx.getRegisterIndex(), nn.getIntValue()));
                    } else {
                        throw new OctoAssemblerException("Operand '%d' for 'vx :=' does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(nn.getIntValue()), nn.getSourcePosition());
                    }
                }
                case AssignmentKeywordToken assignmentKeywordToken -> switch (assignmentKeywordToken.getAssignmentKeyword()) {
                    case DELAY -> List.of(new SetRegisterToDelayTimerAssignment(parserContext.getHere(), vx.getRegisterIndex()));
                    case KEY -> List.of(new SetRegisterToKeyAssignment(parserContext.getHere(), vx.getRegisterIndex()));
                    case RANDOM -> {
                        NumberLiteralToken nn = parserContext.pollTokenOrThrow(NumberLiteralToken.class, "Expected number operand after 'vx := random' assignment!", vx.getSourcePosition(), token -> "The argument '%s' is not an integer!".formatted(token.getLexeme()));
                        if (nn.is8Bits()) {
                            yield List.of(new SetRegisterToRandomAssignment(parserContext.getHere(), vx.getRegisterIndex(), nn.getIntValue()));
                        } else {
                            throw new OctoAssemblerException("Operand '%d' for 'vx := random' does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(nn.getIntValue()), nn.getSourcePosition());
                        }
                    }
                    default -> parserContext.checkReservedName(assignmentKeywordToken);
                };
                case Token token -> throw new OctoAssemblerException("Unexpected operand '%s' for 'vx :=' statement".formatted(token.getLexeme()), token.getSourcePosition());
            };
        };
    }

    private Collection<CodeElement> parseIndexRegister(ParserContext parserContext, IndexRegisterToken indexRegisterToken) throws OctoAssemblerException {
        AssignmentOperatorToken assignmentOperatorToken = parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operators ':=' or '+=' after an 'i' assignment!", indexRegisterToken.getSourcePosition(), token -> "Unknown assignment operator '%s' for 'i' assignment!".formatted(token.getLexeme()));
        return switch (assignmentOperatorToken.getAssignmentOperation()) {
            case ADD -> List.of(new AddRegisterToIndexRegisterAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' register operand after 'i' increment statement!", assignmentOperatorToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case SET -> switch (parserContext.pollTokenOrThrow("Expected operand after 'i' assignment!", assignmentOperatorToken.getSourcePosition())) {
                case NumberLiteralToken nnn -> {
                    if (nnn.isUnsigned12Bits()) {
                        yield List.of(new SetIndexRegisterToConstantAssignment(parserContext.getHere(), new AddressArgument.Resolved(nnn.getIntValue())));
                    } else {
                        throw new OctoAssemblerException("Operand '%d' for 'i' assignment does not fit in 12 bits!".formatted(nnn.getIntValue()), nnn.getSourcePosition());
                    }
                }
                case AssignmentKeywordToken assignmentKeywordToken -> switch (assignmentKeywordToken.getAssignmentKeyword()) {
                    case HEX -> List.of(new SetIndexRegisterToHexCharAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' register operand after 'i := hex' assignment!", assignmentKeywordToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                    case BIGHEX -> List.of(new SetIndexRegisterToBigHexCharAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' register operand after 'i := bighex' increment statement!", assignmentKeywordToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                    case LONG -> {
                        Token longAssignmentArgumentToken = parserContext.pollTokenOrThrow("Expected operand after 'i := long' assignment!", assignmentOperatorToken.getSourcePosition());
                        if (longAssignmentArgumentToken instanceof IntegerLiteralToken nnnn) {
                            if (nnnn.isUnsigned16Bits()) {
                                yield List.of(new SetIndexRegisterToLongConstantAssignment(parserContext.getHere(), new AddressArgument.Resolved(nnnn.getIntValue())));
                            } else {
                                throw new OctoAssemblerException("Operand '%d' for 'i := long' assignment does not fit in 16 bits!".formatted(nnnn.getIntValue()), nnnn.getSourcePosition());
                            }
                        } else {
                            yield List.of(new SetIndexRegisterToLongConstantAssignment(parserContext.getHere(), new AddressArgument.NamedLabelReference(longAssignmentArgumentToken)));
                        }
                    }
                    default -> throw new OctoAssemblerException("Unexpected operand '%s' after 'i' assignment statement!".formatted(assignmentKeywordToken.getLexeme()), assignmentKeywordToken.getSourcePosition());
                };
                case Token labelToken -> List.of(new SetIndexRegisterToConstantAssignment(parserContext.getHere(), new AddressArgument.NamedLabelReference(labelToken)));
            };
            default -> throw new OctoAssemblerException("Unknown assignment operator '%s' after 'i' assignment!".formatted(assignmentOperatorToken.getLexeme()), assignmentOperatorToken.getSourcePosition());
        };
    }

    private Collection<CodeElement> parseDirective(ParserContext parserContext, DirectiveToken directiveToken) throws OctoAssemblerException {
        return switch (directiveToken.getDirective()) {
            case PROTO -> {
                // Deprecated directive. Just discard the following token
                parserContext.pollToken();
                yield List.of();
            }
            case LABEL_DEFINITION -> {
                Token labelNameToken = parserContext.pollTokenOrThrow("Expected label name following ':' directive!", directiveToken.getSourcePosition());
                parserContext.addDirectiveDefinition(labelNameToken, new LabelDefinition(labelNameToken.getLexeme(), parserContext.getHere()));
                yield List.of();
            }
            case NEXT -> {
                Token nextLabelToken = parserContext.pollTokenOrThrow("Expected label name following ':next' directive!", directiveToken.getSourcePosition());
                parserContext.addDirectiveDefinition(nextLabelToken, new NextDefinition(nextLabelToken.getLexeme(), parserContext.addToHere(nextLabelToken, 1)));
                yield List.of();
            }
            case CALC -> {
                Token calcDirectiveNameToken = parserContext.pollTokenOrThrow("Expected name after ':calc' directive!", directiveToken.getSourcePosition());
                DelimiterToken delimiterToken = parserContext.pollTokenOrThrow(DelimiterToken.class, "Expected opening '{' to begin calculated expression!", calcDirectiveNameToken.getSourcePosition(), _ -> "Expected opening '{' to begin calculated expression!");
                parserContext.addDirectiveDefinition(calcDirectiveNameToken, new CalcDefinition(calcDirectiveNameToken.getLexeme(), this.parseCalcExpression(parserContext, delimiterToken)));
                yield List.of();
            }
            case BYTE -> switch (parserContext.pollTokenOrThrow("Expected byte value or calculated expression after ':byte' directive!", directiveToken.getSourcePosition())) {
                case NumberLiteralToken numberLiteralToken -> {
                    if (numberLiteralToken.is8Bits()) {
                        yield List.of(new ByteElement(parserContext.getHere(), numberLiteralToken.getIntValue()));
                    } else {
                        throw new OctoAssemblerException("Raw integer literal '%d' does not fit in 8 bits [-128, 255]".formatted(numberLiteralToken.getIntValue()), numberLiteralToken.getSourcePosition());
                    }
                }
                case DelimiterToken delimiterToken -> List.of(new ByteElement(parserContext.getHere(), toUInt8JS(this.parseCalcExpression(parserContext, delimiterToken))));
                case Token token -> throw new OctoAssemblerException("Unknown ':byte' directive argument '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
            };
            case ORG -> {
                switch (parserContext.pollTokenOrThrow("Expected address value or calculated expression after ':org' directive!", directiveToken.getSourcePosition())) {
                    case NumberLiteralToken numberLiteralToken -> {
                        if (numberLiteralToken.isUnsigned16Bits()) {
                            parserContext.setHere(numberLiteralToken.getIntValue());
                        } else {
                            throw new OctoAssemblerException("The ':org' argument value '%d' does not fit in 16 bits!".formatted(numberLiteralToken.getIntValue()), numberLiteralToken.getSourcePosition());
                        }
                    }
                    case DelimiterToken delimiterToken -> parserContext.setHere(toInt32JS(this.parseCalcExpression(parserContext, delimiterToken)) & 0xFFFF);
                    case Token token -> throw new OctoAssemblerException("Unknown ':org' directive argument '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
                }
                yield List.of();
            }
            case CALL -> switch (parserContext.pollTokenOrThrow("Expected number or calculated expression after ':call' directive!", directiveToken.getSourcePosition())) {
                case NumberLiteralToken numberLiteralToken -> {
                    if (numberLiteralToken.isUnsigned12Bits()) {
                        yield List.of(new CallStatement(parserContext.getHere(), new AddressArgument.Resolved(numberLiteralToken.getIntValue() & 0xFFF)));
                    } else {
                        throw new OctoAssemblerException("Operand '%d' for ':call' directive does not fit in 12 bits!".formatted(numberLiteralToken.getIntValue()), numberLiteralToken.getSourcePosition());
                    }
                }
                case DelimiterToken delimiterToken -> List.of(new CallStatement(parserContext.getHere(), new AddressArgument.Resolved(toInt32JS(this.parseCalcExpression(parserContext, delimiterToken)) & 0xFFF)));
                case Token token -> List.of(new CallStatement(parserContext.getHere(), new AddressArgument.NamedLabelReference(token)));
            };
            case ALIAS -> {
                Token aliasNameToken = parserContext.pollTokenOrThrow("Expected name after ':alias' directive!", directiveToken.getSourcePosition());
                switch (parserContext.pollTokenOrThrow("Expected register or calculated expression argument in 'alias' directive!", aliasNameToken.getSourcePosition())) {
                    case RegisterLiteralToken registerLiteralToken -> parserContext.addDirectiveDefinition(aliasNameToken, new AliasDefinition(aliasNameToken.getLexeme(), registerLiteralToken.getRegisterIndex()));
                    case DelimiterToken delimiterToken -> {
                        int registerIndex = toUInt8JS(this.parseCalcExpression(parserContext, delimiterToken));
                        if (registerIndex >= 0 && registerIndex <= 15) {
                            parserContext.addDirectiveDefinition(aliasNameToken, new AliasDefinition(aliasNameToken.getLexeme(), registerIndex));
                        } else {
                            throw new OctoAssemblerException("Register index argument '%d' for 'alias' directive must be in the range [0, 15]!".formatted(registerIndex), delimiterToken.getSourcePosition());
                        }
                    }
                    case Token aliasArgumentToken -> throw new OctoAssemblerException("Unexpected argument '%s' for ':alias' directive!".formatted(aliasArgumentToken.getLexeme()), aliasArgumentToken.getSourcePosition());
                }
                yield List.of();
            }
            case CONST -> {
                Token constNameToken = parserContext.pollTokenOrThrow("Expected name after ':const' directive!", directiveToken.getSourcePosition());
                switch (parserContext.pollTokenOrThrow("Expected number or label argument in ':const' directive!", constNameToken.getSourcePosition())) {
                    case NumberLiteralToken numberLiteralToken -> parserContext.addDirectiveDefinition(constNameToken, new ConstDefinition(constNameToken.getLexeme(), numberLiteralToken.getIntValue()));
                    case Token constDirectiveNameArgumentToken -> {
                        Optional<DirectiveDefinition> directiveArgument = parserContext.getDirective(constDirectiveNameArgumentToken);
                        if (directiveArgument.isPresent()) {
                            switch (directiveArgument.get()) {
                                case LabelDefinition labelDefinition -> parserContext.addDirectiveDefinition(constNameToken, new ConstDefinition(constNameToken.getLexeme(), labelDefinition.getAddress()));
                                case ConstDefinition constDefinition -> parserContext.addDirectiveDefinition(constNameToken, new ConstDefinition(constNameToken.getLexeme(), constDefinition.getValue()));
                                default -> throw new OctoAssemblerException("Undefined constant name '%s'!".formatted(constDirectiveNameArgumentToken.getLexeme()), constDirectiveNameArgumentToken.getSourcePosition());
                            }
                        } else {
                            throw new OctoAssemblerException("Undefined constant name '%s'!".formatted(constDirectiveNameArgumentToken.getLexeme()), constDirectiveNameArgumentToken.getSourcePosition());
                        }
                    }
                }
                yield List.of();
            }
            case POINTER -> switch (parserContext.pollTokenOrThrow("Expected number, label, or calculated expression after ':pointer' directive!", directiveToken.getSourcePosition())) {
                case NumberLiteralToken numberLiteralToken -> {
                    if (numberLiteralToken.isUnsigned16Bits()) {
                        yield List.of(new PointerElement(parserContext.getHere(), new AddressArgument.Resolved(numberLiteralToken.getIntValue())));
                    } else {
                        throw new OctoAssemblerException("Number argument '%d' does not fit in 8 bits [-128, 255]".formatted(numberLiteralToken.getIntValue()), numberLiteralToken.getSourcePosition());
                    }
                }
                case Token token -> {
                    if (token instanceof DelimiterToken delimiterToken && delimiterToken.getDelimiter() == Delimiter.BRACE && delimiterToken.getDelimiterType() == Delimiter.Type.OPENING) {
                        yield List.of(new PointerElement(parserContext.getHere(), new AddressArgument.Resolved(toInt32JS(this.parseCalcExpression(parserContext, delimiterToken)) & 0xFFFF)));
                    } else {
                        yield List.of(new PointerElement(parserContext.getHere(), new AddressArgument.NamedLabelReference(token)));
                    }
                }
            };
            case UNPACK -> {
                Token nibbleOrLongToken = parserContext.pollTokenOrThrow("Expected nibble or 'long' argument in ':unpack' directive!", directiveToken.getSourcePosition());
                Token labelToken = parserContext.pollTokenOrThrow("Expected label argument in ':unpack' directive!", nibbleOrLongToken.getSourcePosition());
                parserContext.checkReservedName(labelToken);

                int highRegisterIndex = parserContext.getDirective("unpack-hi").map(directiveDefinition -> {
                    if (directiveDefinition instanceof AliasDefinition aliasDefinition) {
                        return aliasDefinition;
                    } else {
                        return null;
                    }
                }).map(AliasDefinition::getRegisterIndex).orElse(0);

                int lowRegisterIndex = parserContext.getDirective("unpack-lo").map(directiveDefinition -> {
                    if (directiveDefinition instanceof AliasDefinition aliasDefinition) {
                        return aliasDefinition;
                    } else {
                        return null;
                    }
                }).map(AliasDefinition::getRegisterIndex).orElse(1);

                AddressArgument addressArgument = switch (labelToken) {
                    case NumberLiteralToken numberLiteralToken -> {
                        if (numberLiteralToken.isUnsigned16Bits()) {
                            yield new AddressArgument.Resolved(numberLiteralToken.getIntValue());
                        } else {
                            throw new OctoAssemblerException("The ':unpack' directive argument '%d' does not fit in 16 bits!".formatted(numberLiteralToken.getIntValue()), numberLiteralToken.getSourcePosition());
                        }
                    }
                    case Token token -> new AddressArgument.NamedLabelReference(token);
                };

                yield switch (nibbleOrLongToken) {
                    case NumberLiteralToken numberLiteralToken -> {
                        if (numberLiteralToken.isUnsigned4Bits()) {
                            yield List.of(new UnpackElement(parserContext.getHere(), highRegisterIndex, lowRegisterIndex, numberLiteralToken.getIntValue(), addressArgument));
                        } else {
                            throw new OctoAssemblerException("The ':unpack' directive operand %d does not fit in 4 bits. Must be in the range [0, 15]!".formatted(numberLiteralToken.getIntValue()), numberLiteralToken.getSourcePosition());
                        }
                    }
                    case AssignmentOnlyKeywordToken assignmentOnlyKeywordToken -> {
                        if (assignmentOnlyKeywordToken.getAssignmentKeyword() == AssignmentKeyword.LONG) {
                            yield List.of(new UnpackElement(parserContext.getHere(), highRegisterIndex, lowRegisterIndex, addressArgument));
                        } else {
                            throw new OctoAssemblerException("Unexpected ':unpack' argument keyword '%s'!".formatted(assignmentOnlyKeywordToken.getLexeme()), assignmentOnlyKeywordToken.getSourcePosition());
                        }
                    }
                    case Token token -> throw new OctoAssemblerException("Unexpected ':unpack' argument '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
                };
            }
            case MACRO -> throw new OctoAssemblerException("The ':macro' directive is not yet supported!", directiveToken.getSourcePosition());
            case ASSERT -> throw new OctoAssemblerException("The ':assert' directive is not yet supported!", directiveToken.getSourcePosition());
            case MONITOR -> throw new OctoAssemblerException("The ':monitor' directive is not yet supported!", directiveToken.getSourcePosition());
            case BREAKPOINT -> throw new OctoAssemblerException("The ':breakpoint' directive is not yet supported!", directiveToken.getSourcePosition());
            case STRING_MODE -> throw new OctoAssemblerException("The ':stringmode' directive is not yet supported!", directiveToken.getSourcePosition());
        };
    }

    private double parseCalcExpression(ParserContext parserContext, DelimiterToken delimiterToken) throws OctoAssemblerException {
        if (delimiterToken.getDelimiter() != Delimiter.BRACE || delimiterToken.getDelimiterType() != Delimiter.Type.OPENING) {
            throw new OctoAssemblerException("Unexpected delimiter token '%s' to begin calculated expression!".formatted(delimiterToken.getLexeme()), delimiterToken.getSourcePosition());
        }
        double value = this.evaluateCalcExpression(parserContext);
        DelimiterToken endDelimiterToken = parserContext.pollTokenOrThrow(DelimiterToken.class, "Expected closing brace '}' to end calculated expression!", delimiterToken.getSourcePosition(), token -> "Unexpected delimiter token '%s' to end calculated expression!".formatted(token.getSourcePosition()));
        if (endDelimiterToken.getDelimiter() != Delimiter.BRACE || endDelimiterToken.getDelimiterType() != Delimiter.Type.CLOSING) {
            throw new OctoAssemblerException("Unexpected delimiter token '%s' to end calculated expression!".formatted(endDelimiterToken.getLexeme()), endDelimiterToken.getSourcePosition());
        }
        return value;
    }

    private double evaluateCalcExpression(ParserContext parserContext) throws OctoAssemblerException {
        Token currentToken = parserContext.pollTokenOrThrow("Expected token in calc expression!", null);
        if (currentToken instanceof CalcOperatorToken calcOperatorToken) {
            switch (calcOperatorToken.getCalcOperation()) {
                case STRLEN -> {
                    StringLiteralToken stringLiteralToken = parserContext.pollTokenOrThrow(StringLiteralToken.class, "Expected string literal token!", currentToken.getSourcePosition(), actualToken -> "The argument '%s' is not a string literal!".formatted(actualToken.getLexeme()));
                    return stringLiteralToken.getResolved().length();
                }
                case MINUS -> {
                    return -this.evaluateCalcExpression(parserContext);
                }
                case BITWISE_NOT -> {
                    return ~toInt32JS(this.evaluateCalcExpression(parserContext));
                }
                case LOGICAL_NOT -> {
                    return toInt32JS(this.evaluateCalcExpression(parserContext)) == 0 ? 1 : 0;
                }
                case SIN -> {
                    return Math.sin(this.evaluateCalcExpression(parserContext));
                }
                case COS -> {
                    return Math.cos(this.evaluateCalcExpression(parserContext));
                }
                case TAN -> {
                    return Math.tan(this.evaluateCalcExpression(parserContext));
                }
                case EXP -> {
                    return Math.exp(this.evaluateCalcExpression(parserContext));
                }
                case LOG -> {
                    return Math.log(this.evaluateCalcExpression(parserContext));
                }
                case ABS -> {
                    return Math.abs(this.evaluateCalcExpression(parserContext));
                }
                case SQRT -> {
                    return Math.sqrt(this.evaluateCalcExpression(parserContext));
                }
                case SIGN -> {
                    double value = this.evaluateCalcExpression(parserContext);
                    if (value > 0) {
                        return 1;
                    } else if (value < 0) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
                case CEIL -> {
                    return Math.ceil(this.evaluateCalcExpression(parserContext));
                }
                case FLOOR -> {
                    return Math.floor(this.evaluateCalcExpression(parserContext));
                }
                case AT -> {
                    return (int) parserContext.getROMByte(toInt32JS(this.evaluateCalcExpression(parserContext))) & 0xFF;
                }
            }
        }
        double value = this.parseCalcTerminal(parserContext, currentToken);
        Token nextToken = parserContext.pollTokenOrThrow("Expected token in calc expression!", currentToken.getSourcePosition());
        if (nextToken instanceof CalcOperatorToken calcOperatorToken) {
            switch (calcOperatorToken.getCalcOperation()) {
                case MINUS -> {
                    return value - this.evaluateCalcExpression(parserContext);
                }
                case PLUS -> {
                    return value + this.evaluateCalcExpression(parserContext);
                }
                case MULTIPLY -> {
                    return value * this.evaluateCalcExpression(parserContext);
                }
                case DIVIDE -> {
                    return value / this.evaluateCalcExpression(parserContext);
                }
                case MODULO -> {
                    return value % this.evaluateCalcExpression(parserContext);
                }
                case BITWISE_AND -> {
                    return toInt32JS(value) & toInt32JS(this.evaluateCalcExpression(parserContext));
                }
                case BITWISE_OR -> {
                    return toInt32JS(value) | toInt32JS(this.evaluateCalcExpression(parserContext));
                }
                case BITWISE_XOR -> {
                    return toInt32JS(value) ^ toInt32JS(this.evaluateCalcExpression(parserContext));
                }
                case LEFT_SHIFT -> {
                    return toInt32JS(value) << toInt32JS(this.evaluateCalcExpression(parserContext));
                }
                case RIGHT_SHIFT -> {
                    return toInt32JS(value) >> toInt32JS(this.evaluateCalcExpression(parserContext));
                }
                case POW -> {
                    return Math.pow(value, this.evaluateCalcExpression(parserContext));
                }
                case MIN -> {
                    return Math.min(value, this.evaluateCalcExpression(parserContext));
                }
                case MAX -> {
                    return Math.max(value, this.evaluateCalcExpression(parserContext));
                }
                case LESS_THAN -> {
                    return value < this.evaluateCalcExpression(parserContext) ? 1 : 0;
                }
                case GREATER_THAN -> {
                    return value > this.evaluateCalcExpression(parserContext) ? 1 : 0;
                }
                case LESS_THAN_THAN_OR_EQUALS -> {
                    return value <= this.evaluateCalcExpression(parserContext) ? 1 : 0;
                }
                case GREATER_THAN_OR_EQUALS -> {
                    return value >= this.evaluateCalcExpression(parserContext) ? 1 : 0;
                }
                case EQUALS -> {
                    return value == this.evaluateCalcExpression(parserContext) ? 1 : 0;
                }
                case NOT_EQUALS -> {
                    return value != this.evaluateCalcExpression(parserContext) ? 1 : 0;
                }
                default -> parserContext.getTokenStream().offerFront(calcOperatorToken);
            }
        } else {
            parserContext.getTokenStream().offerFront(nextToken);
        }
        return value;
    }

    private double parseCalcTerminal(ParserContext parserContext, Token token) throws OctoAssemblerException {
        return switch (token) {
            case CalcConstantToken calcConstantToken -> switch (calcConstantToken.getCalcConstant()) {
                case E -> Math.E;
                case PI -> Math.PI;
                case HERE -> parserContext.getHere();
            };
            case RegisterLiteralToken registerLiteralToken -> registerLiteralToken.getRegisterIndex();
            case NumberLiteralToken numberLiteralToken -> numberLiteralToken.getDoubleValue();
            case Token terminalToken -> {
                Optional<DirectiveDefinition> directiveDefinition = parserContext.getDirective(terminalToken);
                if (directiveDefinition.isPresent()) {
                    switch (directiveDefinition.get()) {
                        case LabelDefinition labelDefinition -> {
                            yield labelDefinition.getAddress();
                        }
                        case ConstDefinition constDefinition -> {
                            yield constDefinition.getValue();
                        }
                        case CalcDefinition calcDefinition -> {
                            yield calcDefinition.getValue();
                        }
                        default -> {}
                    }
                }
                if (!(terminalToken instanceof DelimiterToken delimiterToken) || delimiterToken.getDelimiter() != Delimiter.PARENTHESIS || delimiterToken.getDelimiterType() != Delimiter.Type.OPENING) {
                    throw new OctoAssemblerException("Found undefined name '%s' when calculating constant expression!".formatted(terminalToken.getLexeme()), terminalToken.getSourcePosition());
                }
                double value = this.evaluateCalcExpression(parserContext);
                DelimiterToken endDelimiterToken = parserContext.pollTokenOrThrow(DelimiterToken.class, "Expected closing parenthesis ')' to end calculated segment!", delimiterToken.getSourcePosition(), actualToken -> "Unexpected delimiter token '%s' to end calculated expression segment!".formatted(actualToken.getSourcePosition()));
                if (endDelimiterToken.getDelimiter() != Delimiter.PARENTHESIS || endDelimiterToken.getDelimiterType() != Delimiter.Type.CLOSING) {
                    throw new OctoAssemblerException("Unexpected delimiter token '%s' to end calculated expression segment!".formatted(endDelimiterToken.getLexeme()), endDelimiterToken.getSourcePosition());
                }
                yield value;
            }
        };
    }

    private Collection<CodeElement> invertSkipConditions(Collection<CodeElement> codeElements) {
        List<CodeElement> invertedSkips = new ArrayList<>();
        for (CodeElement primitive : codeElements) {
            if (primitive instanceof SkipInstructionElement skipInstruction) {
                invertedSkips.add(skipInstruction.invertCondition());
            } else {
                invertedSkips.add(primitive);
            }
        }
        return invertedSkips;
    }

    private class ParserContext {

        private final SourceStream<Token> tokenStream;
        private final List<CodeElement> codeElements = new ArrayList<>();
        private final Map<String, DirectiveDefinition> directiveDefinitions = new HashMap<>();
        private final Map<InternalLabelKey, Integer> internalLabelDefinitions = new HashMap<>();
        private final Stack<InternalLabelKey> ifStack = new Stack<>();
        private final Stack<InternalLabelKey> loopStack = new Stack<>();
        private int here = programStart;
        private boolean foundMainLabel;

        private ParserContext(SourceStream<Token> tokenStream) {
            this.tokenStream = tokenStream;
        }

        public SourceStream<Token> getTokenStream() {
            return this.tokenStream;
        }

        public int getHere() {
            return this.here;
        }

        private Optional<DirectiveDefinition> getDirective(Token token) {
            return this.getDirective(token.getLexeme());
        }

        private Optional<DirectiveDefinition> getDirective(String name) {
            return Optional.ofNullable(this.directiveDefinitions.get(name));
        }

        public Collection<CodeElement> getCodeElements() {
            return List.copyOf(this.codeElements);
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

        private Map<InternalLabelKey, Integer> getInternalLabelDefinitions() {
            return Map.copyOf(this.internalLabelDefinitions);
        }

        private boolean foundMainLabel() {
            return this.foundMainLabel;
        }

        private void addCodeElement(CodeElement codeElement) throws OctoAssemblerException {
            if (codeElement instanceof LabelableElement labelableElement && labelableElement.getAddressArgument() instanceof AddressArgument.NamedLabelReference(Token token)) {
                this.checkReservedName(token);
            }
            this.codeElements.add(codeElement);
        }

        private void addDirectiveDefinition(Token token, DirectiveDefinition directiveDefinition) throws OctoAssemblerException {
            String name = token.getLexeme();
            DirectiveDefinition existingDirective = this.directiveDefinitions.get(name);
            if (existingDirective != null && !(existingDirective instanceof AliasDefinition) && !(existingDirective instanceof CalcDefinition)) {
                throw new OctoAssemblerException("Directive name '%s' is already defined!".formatted(name), token.getSourcePosition());
            }
            this.checkReservedName(token);
            if (directiveDefinition instanceof LabelDefinition labelDefinition && "main".equals(labelDefinition.getName())) {
                this.foundMainLabel = true;
                if (labelDefinition.getAddress() == programStart + 2 || labelDefinition.getAddress() == programStart) {
                    this.here = programStart;
                    this.codeElements.clear();
                    directiveDefinition = new LabelDefinition("main", programStart);
                }
            }
            this.directiveDefinitions.put(name, directiveDefinition);
        }

        private void addInternalLabelDefinition(InternalLabelKey internalLabelKey, int addressValue) {
            if (this.internalLabelDefinitions.containsKey(internalLabelKey) && this.internalLabelDefinitions.get(internalLabelKey) != addressValue) {
                throw new IllegalArgumentException("Tried to insert internal label definition with key '%s' because it is already mapped to '%d'!".formatted(internalLabelKey, this.internalLabelDefinitions.get(internalLabelKey)));
            }
            this.internalLabelDefinitions.put(internalLabelKey, addressValue);
        }

        private void setHere(int value) {
            if (value < 0 || value > 0xFFFF) {
                throw new IllegalArgumentException("Tried to set 'here' to a negative value or value greater than the 16-bit integer limit!");
            }
            this.here = value;
        }

        private int addToHere(@Nullable Token token, int amount) throws OctoAssemblerException {
            int newHere = this.here + amount;
            if (newHere > 0xFFFF) {
                throw new OctoAssemblerException("ROM size exceeds the 16-bit integer limit!", token == null ? null : token.getSourcePosition());
            }
            return newHere;
        }

        private void incrementHere(@Nullable Token token, int amount) throws OctoAssemblerException {
            this.here = this.addToHere(token, amount);
        }

        private void incrementHere(@Nullable Token token, CodeElement codeElement) throws OctoAssemblerException {
            this.incrementHere(token, codeElement.getSizeInBytes());
        }

        private void incrementHere(CodeElement codeElement) throws OctoAssemblerException {
            this.incrementHere(null, codeElement);
        }

        private void pushIf(SourcePosition sourcePositionKey) {
            this.ifStack.push(new InternalLabelKey(this.here, sourcePositionKey));
        }

        private Optional<InternalLabelKey> peekIf() {
            return this.ifStack.isEmpty() ? Optional.empty() : Optional.of(this.ifStack.peek());
        }

        private Optional<InternalLabelKey> popIf() {
            return this.ifStack.isEmpty() ? Optional.empty() : Optional.of(this.ifStack.pop());
        }

        private void pushLoop(SourcePosition sourcePositionKey) {
            this.loopStack.push(new InternalLabelKey(this.here, sourcePositionKey));
        }

        private Optional<InternalLabelKey> peekLoop() {
            return this.loopStack.isEmpty() ? Optional.empty() : Optional.of(this.loopStack.peek());
        }

        private Optional<InternalLabelKey> popLoop() {
            return this.loopStack.isEmpty() ? Optional.empty() : Optional.of(this.loopStack.pop());
        }

        private Optional<Token> pollToken() {
            return this.tokenStream.poll().flatMap(this::resolveToken);
        }

        private Token pollTokenOrThrow(String error, SourcePosition sourcePosition) throws OctoAssemblerException {
            return this.pollToken().orElseThrow(() -> new OctoAssemblerException(error, sourcePosition));
        }

        @SuppressWarnings("unchecked")
        private <T extends Token> T pollTokenOrThrow(Class<T> tokenClass, String errorIfNotPresent, SourcePosition sourcePositionIfNotPresent, Function<Token, String> errorIfTokenNotInstance) throws OctoAssemblerException {
            Token token = this.pollTokenOrThrow(errorIfNotPresent, sourcePositionIfNotPresent);
            if (tokenClass.isInstance(token)) {
                return (T) token;
            } else {
                throw new OctoAssemblerException(errorIfTokenNotInstance.apply(token), token.getSourcePosition());
            }
        }

        private Optional<Token> peekToken() {
            Optional<Token> peekedToken = this.pollToken();
            peekedToken.ifPresent(this.tokenStream::offerFront);
            return peekedToken;
        }

        @SuppressWarnings("unchecked")
        private <T extends Token> Optional<T> peekToken(Class<T> tokenClass) {
            return this.peekToken().map(token -> {
                if (tokenClass.isInstance(token)) {
                    return (T) token;
                } else {
                    return null;
                }
            });
        }

        @SuppressWarnings("SameParameterValue")
        private <T extends Token> Optional<T> peekTokenAndPollIfPresent(Class<T> tokenClass) {
            return this.peekToken(tokenClass).map(token -> {
                this.pollToken();
                return token;
            });
        }

        private Optional<Token> resolveToken(Token token) {
            if (token instanceof IdentifierToken identifierToken) {
                Optional<DirectiveDefinition> directiveDefinition = this.getDirective(identifierToken);
                if (directiveDefinition.isPresent() && directiveDefinition.get() instanceof ExpandableDirective expandableDirective) {
                    this.expandDirective(expandableDirective, identifierToken.getSourcePosition());
                    return this.pollToken();
                }
            }
            return Optional.of(token);
        }

        private void expandDirective(ExpandableDirective expandableDirective, SourcePosition sourcePosition) {
            List<Token> directiveTokens = expandableDirective.expand(this.here, sourcePosition);
            for (int i = directiveTokens.size() - 1; i >= 0; i--) {
                this.tokenStream.offerFront(directiveTokens.get(i));
            }
        }

        private Collection<CodeElement> checkReservedName(Token token) throws OctoAssemblerException {
            if (token instanceof ReservedNameToken) {
                throw new OctoAssemblerException("The name '%s' is reserved and cannot be used as a label or directive name!".formatted(token.getLexeme()), token.getSourcePosition());
            } else {
                return List.of(new CallStatement(this.here, new AddressArgument.NamedLabelReference(token)));
            }
        }

        private byte getROMByte(int address) {
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
                                    case AddressArgument.NamedLabelReference namedLabelReference -> this.getDirective(namedLabelReference.token())
                                        .map(directive -> {
                                            if (directive instanceof LabelDefinition labelDefinition) {
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

    }

}
