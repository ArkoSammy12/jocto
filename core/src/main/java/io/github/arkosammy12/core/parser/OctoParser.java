package io.github.arkosammy12.core.parser;

import io.github.arkosammy12.core.codegen.*;
import io.github.arkosammy12.core.grammar.IfBlockKeywordLexeme;
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
            while (!parserContext.getTokenStream().isEmpty()) {
                Optional<Token> optionalToken = parserContext.getTokenStream().poll();
                if (optionalToken.isPresent()) {
                    Token token = optionalToken.get();
                    Collection<? extends CodeElement> codeElements = switch (token) {
                        case IfBlockKeywordToken ifBlockKeywordToken -> this.parseIfBlockKeywordToken(parserContext, ifBlockKeywordToken);
                        case LoopBlockKeywordToken loopBlockKeywordToken -> {
                            Collection<CodePrimitive> loopBlockElements = this.parseLoopBlockKeywordToken(parserContext, loopBlockKeywordToken);
                            for (CodePrimitive loopBlockElement : loopBlockElements) {
                                parserContext.incrementHere(loopBlockKeywordToken, loopBlockElement);
                            }
                            yield loopBlockElements;
                        }
                        default -> this.parseTopLevel(parserContext, token);
                    };
                    codeElements.forEach(parserContext::addCodeElement);
                }
            }
            return new ParserResult.Ok(List.copyOf(parserContext.getCodeElements()), parserContext.getLabelDefinitions(), parserContext.getAddressedLabelDefinitions());
        } catch (ParserException e) {
            return e.toErrorResult();
        }
    }

    private Collection<? extends CodeElement> parseTopLevel(ParserContext parserContext, Token token) throws ParserException {
        Collection<CodePrimitive> codePrimitives = this.parseToken(parserContext, token);
        for (CodePrimitive codePrimitive : codePrimitives) {
            parserContext.incrementHere(token, codePrimitive);
        }
        return codePrimitives;
    }

    private Collection<CodeElement> parseIfBlockKeywordToken(ParserContext parserContext, IfBlockKeywordToken ifBlockKeywordToken) throws ParserException {
        if (ifBlockKeywordToken.getIfBlockKeywordLexeme() != IfBlockKeywordLexeme.IF) {
            throw new ParserException("Unmatched '%s' if block keyword!".formatted(ifBlockKeywordToken.getIfBlockKeywordLexeme()), ifBlockKeywordToken.getSourcePosition());
        }
        // HERE is pointing to the first opcode of the conditional expression
        int firstConditionalOpcodeOffset = parserContext.getHere();
        Collection<CodePrimitive> conditionalExpressionOpcodes = this.parseConditionalExpression(parserContext, ifBlockKeywordToken);
        for (CodePrimitive conditionalExpressionOpcode : conditionalExpressionOpcodes) {
            parserContext.incrementHere(ifBlockKeywordToken, conditionalExpressionOpcode);
        }

        IfBlockKeywordToken ifBlockBeginningToken = parserContext.pollTokenOrThrow(IfBlockKeywordToken.class, "Unexpected if block keyword beginning!", ifBlockKeywordToken.getSourcePosition());
        return switch (ifBlockBeginningToken.getIfBlockKeywordLexeme()) {
            case THEN -> {
                // HERE is pointing to the skipped instruction, which is surrounded by the if-then block
                List<CodeElement> ifThenBlockElements = new ArrayList<>();
                Token ifThenToken = null;
                while (ifThenBlockElements.isEmpty() && !parserContext.getTokenStream().isEmpty()) {
                    Optional<Token> optionalToken = parserContext.pollToken();
                    if (optionalToken.isPresent()) {
                        ifThenToken = optionalToken.get();
                        ifThenBlockElements.addAll(switch (ifThenToken) {
                            case IfBlockKeywordToken innerIfBlockKeyword -> this.parseIfBlockKeywordToken(parserContext, innerIfBlockKeyword);
                            case LoopBlockKeywordToken innerLoopBlockKeywordToken -> this.parseLoopBlockKeywordToken(parserContext, innerLoopBlockKeywordToken);
                            case Token innerToken -> this.parseToken(parserContext, innerToken);
                        });
                    }
                }

                if (ifThenBlockElements.isEmpty()) {
                    yield List.of(new IfThenBlock(firstConditionalOpcodeOffset, conditionalExpressionOpcodes));
                } else {
                    CodeElement ifThenBlockElement = ifThenBlockElements.getFirst();
                    if (ifThenBlockElement instanceof CodePrimitive codePrimitive) {
                        parserContext.incrementHere(ifThenToken, codePrimitive);
                    }

                    // HERE is pointing to the instruction after the skipped instruction, ending the if-then block
                    Collection<CodeElement> codeElements = new ArrayList<>();
                    codeElements.add(new IfThenBlock(firstConditionalOpcodeOffset, conditionalExpressionOpcodes, ifThenBlockElement));

                    for (int i = 1; i < ifThenBlockElements.size(); i++) {
                        CodeElement c = ifThenBlockElements.get(i);
                        if (c instanceof CodePrimitive codePrimitive) {
                            parserContext.incrementHere(ifThenToken, codePrimitive);
                        }
                        codeElements.add(c);
                    }
                    yield codeElements;
                }
            }
            case BEGIN -> {
                // Invert the skip instruction since now the skip instruction will surround the jump instruction that
                // prevents the if block from being executed, and not the execution of the if block itself
                conditionalExpressionOpcodes = this.invertSkips(conditionalExpressionOpcodes);

                // HERE is pointing to the jump instruction that will jump to the end if the if-begin block or to the
                // else case of the if-else block
                JumpStatement jumpAboveIfBlockStatement = new JumpStatement(parserContext.getHere(), new AddressArgument.AddressedLabelReference(parserContext.getHere()));
                parserContext.incrementHere(ifBlockBeginningToken, jumpAboveIfBlockStatement);

                // HERE now points to the first instruction of the if block within the if-begin or if-else block
                // From now on we are looking for an 'end' or an 'else' followed by an 'end'
                List<CodeElement> ifBlockElements = new ArrayList<>();
                List<CodeElement> elseBlockElements = null;
                JumpStatement jumpAboveElseBlockStatement = null;
                while (!parserContext.getTokenStream().isEmpty()) {
                    Optional<Token> optionalToken = parserContext.pollToken();
                    if (optionalToken.isPresent()) {
                        Token token = optionalToken.get();
                        Collection<? extends CodeElement> codeElements;
                        if (token instanceof IfBlockKeywordToken innerIfBlockKeyword) {
                            switch (innerIfBlockKeyword.getIfBlockKeywordLexeme()) {
                                case ELSE -> {
                                    // HERE is pointing to the jump instruction at the end of the if block,
                                    // that jumps to after the else block
                                    jumpAboveElseBlockStatement = new JumpStatement(parserContext.getHere(), new AddressArgument.AddressedLabelReference(parserContext.getHere()));
                                    parserContext.incrementHere(token, jumpAboveElseBlockStatement);

                                    // HERE now points to the first instruction of the else block
                                    jumpAboveIfBlockStatement.resolve(parserContext.getHere());

                                    elseBlockElements = new ArrayList<>();

                                    // Just continue to the next iteration now that we've consumed the 'else' token
                                    continue;
                                }
                                case END -> {
                                    // HERE is pointing to the first instruction after the end if the if-begin or if-else block
                                    if (elseBlockElements == null) {
                                        jumpAboveIfBlockStatement.resolve(parserContext.getHere());
                                        yield List.of(new IfBeginEndBlock(firstConditionalOpcodeOffset, conditionalExpressionOpcodes, jumpAboveIfBlockStatement, ifBlockElements));
                                    } else {
                                        // Resolve the jump statement at the end of the if block and before the else block,
                                        // which jumps over the else block
                                        jumpAboveElseBlockStatement.resolve(parserContext.getHere());
                                        yield List.of(new IfElseBlock(firstConditionalOpcodeOffset, conditionalExpressionOpcodes, jumpAboveIfBlockStatement, ifBlockElements, jumpAboveElseBlockStatement, elseBlockElements));
                                    }
                                }
                                default -> codeElements = this.parseIfBlockKeywordToken(parserContext, innerIfBlockKeyword);
                            }
                        } else {
                            codeElements = switch (token) {
                                case LoopBlockKeywordToken innerLoopBlockKeyword -> this.parseLoopBlockKeywordToken(parserContext, innerLoopBlockKeyword);
                                case Token innerToken -> this.parseToken(parserContext, innerToken);
                            };
                        }
                        for (CodeElement codeElement : codeElements) {
                            if (codeElement instanceof CodePrimitive codePrimitive) {
                                parserContext.incrementHere(token, codePrimitive);
                            }
                            Objects.requireNonNullElse(elseBlockElements, ifBlockElements).add(codeElement);
                        }
                    }
                }
                throw new ParserException("Unterminated if block!", ifBlockKeywordToken.getSourcePosition());
            }
            default -> throw new ParserException("Unexpected if block beginning!", ifBlockKeywordToken.getSourcePosition());
        };
    }

    private Collection<CodePrimitive> parseLoopBlockKeywordToken(ParserContext parserContext, LoopBlockKeywordToken loopBlockKeywordToken) throws ParserException {
        return switch (loopBlockKeywordToken.getLoopBlockKeyword()) {
            case LOOP -> {
                parserContext.pushLoop();
                yield List.of();
            }
            case AGAIN -> {
                OptionalInt optionalLoopAddress = parserContext.popLoop();
                if (optionalLoopAddress.isEmpty()) {
                    throw new ParserException("This 'again' does not have a matching 'loop'!", loopBlockKeywordToken.getSourcePosition());
                } else {
                    int loopAddress = optionalLoopAddress.getAsInt();
                    JumpStatement jumpToLoopBeginningStatement = new JumpStatement(parserContext.getHere(), new AddressArgument.Resolved(loopAddress));
                    parserContext.addAddressedLabelDefinition(loopAddress, parserContext.addToHere(loopBlockKeywordToken, jumpToLoopBeginningStatement.getSizeInBytes()));
                    yield List.of(jumpToLoopBeginningStatement);
                }
            }
            case WHILE -> {
                OptionalInt loopAddress = parserContext.peekLoop();
                if (loopAddress.isEmpty()) {
                    throw new ParserException("This 'while' is not within a 'loop'!", loopBlockKeywordToken.getSourcePosition());
                } else {
                    Collection<CodePrimitive> conditionalExpressionOpcodes = this.invertSkips(this.parseConditionalExpression(parserContext, loopBlockKeywordToken));
                    JumpStatement jumpToLoopEndStatement = new JumpStatement(parserContext.addToHere(loopBlockKeywordToken, conditionalExpressionOpcodes.stream().map(CodePrimitive::getSizeInBytes).reduce(Integer::sum).orElse(0)), new AddressArgument.AddressedLabelReference(loopAddress.getAsInt()));
                    List<CodePrimitive> codeElements = new ArrayList<>(conditionalExpressionOpcodes);
                    codeElements.add(jumpToLoopEndStatement);
                    yield List.copyOf(codeElements);
                }
            }
        };
    }

    private Collection<CodePrimitive> invertSkips(Collection<CodePrimitive> codePrimitives) {
        List<CodePrimitive> invertedSkips = new ArrayList<>();
        for (CodePrimitive primitive : codePrimitives) {
            if (primitive instanceof SkipInstruction skipInstruction) {
                invertedSkips.add(skipInstruction.invertCondition());
            } else {
                invertedSkips.add(primitive);
            }
        }
        return List.copyOf(invertedSkips);
    }

    private Collection<CodePrimitive> parseConditionalExpression(ParserContext parserContext, Token token) throws ParserException {
        RegisterLiteralToken vx = parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected vx in conditional expression!", token.getSourcePosition());
        ConditionalOperatorToken conditionalOperatorToken = parserContext.pollTokenOrThrow(ConditionalOperatorToken.class, "Expected conditional operator in conditional statement!", vx.getSourcePosition());
        ConditionalOperation conditionalOperation = conditionalOperatorToken.getConditionalOperation();
        return switch (conditionalOperation) {
            case KEY_PRESSED -> List.of(new SkipIfKeyNotPressed(parserContext.getHere(), vx.getRegisterIndex()));
            case KEY_NOT_PRESSED -> List.of(new SkipIfKeyPressed(parserContext.getHere(), vx.getRegisterIndex()));
            default -> {
                Token argumentToken = parserContext.pollTokenOrThrow("Expected vy or NN argument in conditional expression!", conditionalOperatorToken.getSourcePosition());
                yield switch (argumentToken) {
                    case RegisterLiteralToken vy -> switch (conditionalOperation) {
                        case EQUALITY -> List.of(new SkipIfRegistersNotEqual(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex()));
                        case INEQUALITY -> List.of(new SkipIfRegistersEqual(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex()));
                        case GREATER_THAN -> {
                            CodePrimitive assign = new SetRegisterToRegisterAssignment(parserContext.getHere(), 0xF,  vy.getRegisterIndex());
                            CodePrimitive subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.addToHere(argumentToken, assign.getSizeInBytes()), 0xF, vx.getRegisterIndex());
                            CodePrimitive skip = new SkipIfRegisterNotEqualsConstant(parserContext.addToHere(argumentToken, assign.getSizeInBytes() + subtract.getSizeInBytes()), 0xF, 0x00);
                            yield List.of(assign, subtract, skip);
                        }
                        case GREATER_THAN_OR_EQUALS -> {
                            CodePrimitive assign = new SetRegisterToRegisterAssignment(parserContext.getHere(), 0xF,  vy.getRegisterIndex());
                            CodePrimitive subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.addToHere(argumentToken, assign.getSizeInBytes()), 0xF, vx.getRegisterIndex());
                            CodePrimitive skip = new SkipIfRegisterEqualsConstant(parserContext.addToHere(argumentToken, assign.getSizeInBytes() + subtract.getSizeInBytes()), 0xF, 0x00);
                            yield List.of(assign, subtract, skip);
                        }
                        case LESS_THAN -> {
                            CodePrimitive assign = new SetRegisterToRegisterAssignment(parserContext.getHere(), 0xF,  vy.getRegisterIndex());
                            CodePrimitive subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.addToHere(argumentToken, assign.getSizeInBytes()), 0xF, vx.getRegisterIndex());
                            CodePrimitive skip = new SkipIfRegisterNotEqualsConstant(parserContext.addToHere(argumentToken, assign.getSizeInBytes() + subtract.getSizeInBytes()), 0xF, 0x00);
                            yield List.of(assign, subtract, skip);
                        }
                        case LESS_THAN_OR_EQUALS -> {
                            CodePrimitive assign = new SetRegisterToRegisterAssignment(parserContext.getHere(), 0xF,  vy.getRegisterIndex());
                            CodePrimitive subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.addToHere(argumentToken, assign.getSizeInBytes()), 0xF, vx.getRegisterIndex());
                            CodePrimitive skip = new SkipIfRegisterEqualsConstant(parserContext.addToHere(argumentToken, assign.getSizeInBytes() + subtract.getSizeInBytes()), 0xF, 0x00);
                            yield List.of(assign, subtract, skip);
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + conditionalOperation);
                    };
                    case IntegerLiteralToken nn -> {
                        if (!nn.is8Bits()) {
                            throw new ParserException("Conditional argument '%d' does not fit in 8 bits [-128, 255]".formatted(nn.getValue()), nn.getSourcePosition());
                        }
                        yield switch (conditionalOperation) {
                            case EQUALITY -> List.of(new SkipIfRegisterNotEqualsConstant(parserContext.getHere(), vx.getRegisterIndex(), nn.getValue()));
                            case INEQUALITY -> List.of(new SkipIfRegisterEqualsConstant(parserContext.getHere(), vx.getRegisterIndex(), nn.getValue()));
                            case GREATER_THAN -> {
                                CodePrimitive assign = new SetRegisterToConstantAssignment(parserContext.getHere(), 0xF, nn.getValue());
                                CodePrimitive subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.addToHere(argumentToken, assign.getSizeInBytes()), 0xF, vx.getRegisterIndex());
                                CodePrimitive skip = new SkipIfRegisterNotEqualsConstant(parserContext.addToHere(argumentToken, assign.getSizeInBytes() + subtract.getSizeInBytes()), 0xF, 0x00);
                                yield List.of(assign, subtract, skip);
                            }
                            case GREATER_THAN_OR_EQUALS -> {
                                CodePrimitive assign = new SetRegisterToConstantAssignment(parserContext.getHere(), 0xF, nn.getValue());
                                CodePrimitive subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.addToHere(argumentToken, assign.getSizeInBytes()), 0xF, vx.getRegisterIndex());
                                CodePrimitive skip = new SkipIfRegisterEqualsConstant(parserContext.addToHere(argumentToken, assign.getSizeInBytes() + subtract.getSizeInBytes()), 0xF, 0x00);
                                yield List.of(assign, subtract, skip);
                            }
                            case LESS_THAN -> {
                                CodePrimitive assign = new SetRegisterToConstantAssignment(parserContext.getHere(), 0xF, nn.getValue());
                                CodePrimitive subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.addToHere(argumentToken, assign.getSizeInBytes()), 0xF, vx.getRegisterIndex());
                                CodePrimitive skip = new SkipIfRegisterNotEqualsConstant(parserContext.addToHere(argumentToken, assign.getSizeInBytes() + subtract.getSizeInBytes()), 0xF, 0x00);
                                yield List.of(assign, subtract, skip);
                            }
                            case LESS_THAN_OR_EQUALS -> {
                                CodePrimitive assign = new SetRegisterToConstantAssignment(parserContext.getHere(), 0xF, nn.getValue());
                                CodePrimitive subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.addToHere(argumentToken, assign.getSizeInBytes()), 0xF, vx.getRegisterIndex());
                                CodePrimitive skip = new SkipIfRegisterEqualsConstant(parserContext.addToHere(argumentToken, assign.getSizeInBytes() + subtract.getSizeInBytes()), 0xF, 0x00);
                                yield List.of(assign, subtract, skip);
                            }
                            default -> throw new IllegalStateException("Unexpected value: " + conditionalOperation);
                        };
                    }
                    case Token t -> throw new ParserException("Invalid conditional expression argument '%s'!".formatted(t.getLexeme()), t.getSourcePosition());
                };
            }
        };
    }

    private Collection<CodePrimitive> parseToken(ParserContext parserContext, Token token) throws ParserException  {
        return switch (token) {
            case DirectiveToken directiveToken -> this.parseDirective(parserContext, directiveToken);
            case InstructionStatementNameToken instructionStatementNameToken -> this.parseInstructionStatementNameToken(parserContext, instructionStatementNameToken);
            case LiteralToken literalToken -> this.parseLiteral(parserContext, literalToken);
            case IndexRegisterToken indexRegisterToken -> this.parseIndexRegister(parserContext, indexRegisterToken);
            case AssignmentKeywordToken assignmentKeywordToken -> this.parseAssignmentKeywordToken(parserContext, assignmentKeywordToken);
            default -> parserContext.checkReservedName(token);
        };
    }

    private Collection<CodePrimitive> parseInstructionStatementNameToken(ParserContext parserContext, InstructionStatementNameToken instructionStatementNameToken) throws ParserException {
        return switch (instructionStatementNameToken) {
            case SemicolonToken _ -> List.of(new ReturnStatement(parserContext.getHere()));
            case NonSymbolInstructionStatementKeywordToken nonSymbolInstructionStatementKeywordToken -> switch (nonSymbolInstructionStatementKeywordToken.getInstructionStatementKeyword()) {
                case RETURN -> List.of(new ReturnStatement(parserContext.getHere()));
                case CLEAR -> List.of(new ClearStatement(parserContext.getHere()));
                case BCD -> List.of(new BCDStatement(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected register literal after bcd statement!", instructionStatementNameToken.getSourcePosition()).getRegisterIndex()));
                case SAVE -> {
                    RegisterLiteralToken vx = parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected register literal after save statement!", instructionStatementNameToken.getSourcePosition());
                    Optional<DashToken> optionalDashToken = parserContext.peekTokenAndPollIfPresent(DashToken.class);
                    if (optionalDashToken.isPresent()) {
                        yield List.of(new SaveRegistersStatement(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected register literal after '-' in save instruction!", instructionStatementNameToken.getSourcePosition()).getRegisterIndex()));
                    } else {
                        yield List.of(new SaveRegistersStatement(parserContext.getHere(), vx.getRegisterIndex()));
                    }
                }
                case LOAD -> {
                    RegisterLiteralToken vx = parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected register literal after save instruction", instructionStatementNameToken.getSourcePosition());
                    Optional<DashToken> optionalDashToken = parserContext.peekTokenAndPollIfPresent(DashToken.class);
                    if (optionalDashToken.isPresent()) {
                        yield List.of(new LoadRegistersStatement(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected register literal after '-' in save instruction!", instructionStatementNameToken.getSourcePosition()).getRegisterIndex()));
                    } else {
                        yield List.of(new LoadRegistersStatement(parserContext.getHere(), vx.getRegisterIndex()));
                    }
                }
                case SPRITE -> {
                    RegisterLiteralToken vx = parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' argument after sprite statement!", instructionStatementNameToken.getSourcePosition());
                    RegisterLiteralToken vy = parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' argument after 'vx' argument in sprite statement!", instructionStatementNameToken.getSourcePosition());
                    IntegerLiteralToken n = parserContext.pollTokenOrThrow(IntegerLiteralToken.class, "Expected integer literal after 'vy' argument in sprite statement!", instructionStatementNameToken.getSourcePosition());
                    if (!n.isUnsigned4Bits()) {
                        throw new ParserException("Sprite statement argument %d for 'n' does not fit is not in the range [0, 15]".formatted(n.getValue()), instructionStatementNameToken.getSourcePosition());
                    }
                    yield List.of(new SpriteStatement(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex(), n.getValue()));
                }
                case JUMP -> {
                    Token token = parserContext.pollTokenOrThrow("Expected integer or label argument after jump statement!", instructionStatementNameToken.getSourcePosition());
                    if (token instanceof IntegerLiteralToken n) {
                        if (!n.isUnsigned12Bits()) {
                            throw new ParserException("Jump statement target %d does not fit in 12 bits!".formatted(n.getValue()), instructionStatementNameToken.getSourcePosition());
                        }
                        yield List.of(new JumpStatement(parserContext.getHere(), new AddressArgument.Resolved(n.getValue())));
                    } else {
                        yield List.of(new JumpStatement(parserContext.getHere(), new AddressArgument.NamedLabelReference(token.getLexeme())));
                    }
                }
                case JUMP0 -> {
                    Token token = parserContext.pollTokenOrThrow("Expected integer or label argument after jump0 statement!", instructionStatementNameToken.getSourcePosition());
                    if (token instanceof IntegerLiteralToken n) {
                        if (!n.isUnsigned12Bits()) {
                            throw new ParserException("Jump0 statement target %d does not fit in 12 bits!".formatted(n.getValue()), instructionStatementNameToken.getSourcePosition());
                        }
                        yield List.of(new JumpZeroStatement(parserContext.getHere(), new AddressArgument.Resolved(n.getValue())));
                    } else {
                        yield List.of(new JumpZeroStatement(parserContext.getHere(), new AddressArgument.NamedLabelReference(token.getLexeme())));
                    }
                }
                case HIRES -> List.of(new HiresStatement(parserContext.getHere()));
                case LORES -> List.of(new LoresStatement(parserContext.getHere()));
                case SCROLL_DOWN -> {
                    IntegerLiteralToken n = parserContext.pollTokenOrThrow(IntegerLiteralToken.class, "Expected integer literal after 'vy' argument in scroll-down statement!", instructionStatementNameToken.getSourcePosition());
                    if (!n.isUnsigned4Bits()) {
                        throw new ParserException("Scroll down statement argument %d for 'n' does not fit is not in the range [0, 15]".formatted(n.getValue()), instructionStatementNameToken.getSourcePosition());
                    }
                    yield List.of(new ScrollDownStatement(parserContext.getHere(), n.getValue()));
                }
                case SCROLL_LEFT -> List.of(new ScrollLeftStatement(parserContext.getHere()));
                case SCROLL_RIGHT -> List.of(new ScrollRightStatement(parserContext.getHere()));
                case EXIT -> List.of(new ExitStatement(parserContext.getHere()));
                case SAVE_FLAGS -> List.of(new SaveFlagsStatement(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' argument after saveflags statement!", instructionStatementNameToken.getSourcePosition()).getRegisterIndex()));
                case LOAD_FLAGS -> List.of(new LoadFlagsStatement(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' argument after loadflags statement!", instructionStatementNameToken.getSourcePosition()).getRegisterIndex()));
                case PLANE -> {
                    IntegerLiteralToken n = parserContext.pollTokenOrThrow(IntegerLiteralToken.class, "Expected integer literal after 'vy' argument in plane statement!", instructionStatementNameToken.getSourcePosition());
                    if (!n.isUnsigned4Bits()) {
                        throw new ParserException("Plane statement argument %d for 'n' does not fit is not in the range [0, 15]".formatted(n.getValue()), instructionStatementNameToken.getSourcePosition());
                    }
                    yield List.of(new PlaneStatement(parserContext.getHere(), n.getValue()));
                }
                case AUDIO -> List.of(new AudioStatement(parserContext.getHere()));
                case PITCH -> {
                    AssignmentOperatorToken assignmentOperatorToken = parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operator ':=' after pitch statement!", instructionStatementNameToken.getSourcePosition());
                    if (assignmentOperatorToken.getAssignmentOperation() == AssignmentOperation.SET) {
                        yield List.of(new SetPitchAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected register literal as pitch statement argument!", instructionStatementNameToken.getSourcePosition()).getRegisterIndex()));
                    } else {
                        throw new ParserException("Unexpected operator '%s' after pitch statement".formatted(assignmentOperatorToken.getLexeme()), instructionStatementNameToken.getSourcePosition());
                    }
                }
                case SCROLL_UP -> {
                    IntegerLiteralToken n = parserContext.pollTokenOrThrow(IntegerLiteralToken.class, "Expected integer literal after 'vy' argument in scroll-up statement!", instructionStatementNameToken.getSourcePosition());
                    if (!n.isUnsigned4Bits()) {
                        throw new ParserException("Scroll up statement argument %d for 'n' does not fit is not in the range [0, 15]".formatted(n.getValue()), instructionStatementNameToken.getSourcePosition());
                    }
                    yield List.of(new ScrollUpStatement(parserContext.getHere(), n.getValue()));
                }
            };
        };
    }
    
    private Collection<CodePrimitive> parseLiteral(ParserContext parserContext, LiteralToken literalToken) throws ParserException {
        return switch (literalToken) {
            case RegisterLiteralToken registerLiteralToken -> this.parseRegisterLiteral(parserContext, registerLiteralToken);
            case IntegerLiteralToken integerLiteralToken -> {
                if (!integerLiteralToken.is8Bits()) {
                    throw new ParserException("Raw integer literal '%d' does not fit in 8 bits [-128, 255]".formatted(integerLiteralToken.getValue()), integerLiteralToken.getSourcePosition());
                }
                yield List.of(new ByteLiteral(parserContext.getHere(), integerLiteralToken.getValue()));
            }
            case FloatLiteralToken floatLiteralToken -> parserContext.checkReservedName(floatLiteralToken);
            case StringLiteralToken stringLiteralToken -> parserContext.checkReservedName(stringLiteralToken);
        };
    }

    private Collection<CodePrimitive> parseAssignmentKeywordToken(ParserContext parserContext, AssignmentKeywordToken assignmentKeywordToken) throws ParserException {
        return (switch (assignmentKeywordToken.getAssignmentKeyword()) {
            case DELAY -> {
                if (parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operator ':=' after delay statement!", assignmentKeywordToken.getSourcePosition()).getAssignmentOperation() != AssignmentOperation.SET) {
                    throw new ParserException("Expected assignment operator ':=' after delay statement!", assignmentKeywordToken.getSourcePosition());
                }
                yield List.of(new SetDelayTimerAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' argument after delay statement!", assignmentKeywordToken.getSourcePosition()).getRegisterIndex()));
            }
            case BUZZER -> {
                if (parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operator ':=' after buzzer statement!", assignmentKeywordToken.getSourcePosition()).getAssignmentOperation() != AssignmentOperation.SET) {
                    throw new ParserException("Expected assignment operator ':=' after buzzer statement!", assignmentKeywordToken.getSourcePosition());
                }
                yield List.of(new SetBuzzerTimerAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' argument after delay statement!", assignmentKeywordToken.getSourcePosition()).getRegisterIndex()));
            }
            case PITCH -> {
                if (parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operator ':=' after pitch statement!", assignmentKeywordToken.getSourcePosition()).getAssignmentOperation() != AssignmentOperation.SET) {
                    throw new ParserException("Expected assignment operator ':=' after pitch statement!", assignmentKeywordToken.getSourcePosition());
                }
                yield List.of(new SetPitchAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' argument after delay statement!", assignmentKeywordToken.getSourcePosition()).getRegisterIndex()));
            }
            default -> parserContext.checkReservedName(assignmentKeywordToken);
        });
    }
    
    private Collection<CodePrimitive> parseRegisterLiteral(ParserContext parserContext, RegisterLiteralToken vx) throws ParserException {
        return switch (parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Unknown operator on register assignment statement!", vx.getSourcePosition()).getAssignmentOperation()) {
            case BITWISE_OR -> List.of(new BitwiseOrRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected vy argument after 'vx |=' assignment!", vx.getSourcePosition()).getRegisterIndex()));
            case BITWISE_AND -> List.of(new BitwiseAndRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected vy argument after 'vx &=' assignment!", vx.getSourcePosition()).getRegisterIndex()));
            case BITWISE_XOR -> List.of(new BitwiseXorRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected vy argument after 'vx ^=' assignment!", vx.getSourcePosition()).getRegisterIndex()));
            case RIGHT_SHIFT -> List.of(new RightShiftRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected vy argument after 'vx >>=' assignment!", vx.getSourcePosition()).getRegisterIndex()));
            case LEFT_SHIFT -> List.of(new LeftShiftRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected vy argument after 'vx <<=' assignment!", vx.getSourcePosition()).getRegisterIndex()));
            case RIGHT_SUBTRACT -> List.of(new RightSubtractRegisterFromRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected vy argument after 'vx =-' assignment!", vx.getSourcePosition()).getRegisterIndex()));
            case ADD -> switch (parserContext.pollTokenOrThrow("Unterminated 'vx += ' statement!", vx.getSourcePosition())) {
                case RegisterLiteralToken vy -> List.of(new AddRegisterToRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex()));
                case IntegerLiteralToken n -> {
                    if (n.is8Bits()) {
                        yield List.of(new AddConstantToRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), n.getValue()));
                    } else {
                        throw new ParserException("Argument '%d' for 'vx +=' does not fit in a byte!".formatted(n.getValue()), n.getSourcePosition());
                    }
                }
                case Token token -> throw new ParserException("Unexpected argument '%s' for 'vx +=' statement".formatted(token.getLexeme()), token.getSourcePosition());
            };
            case LEFT_SUBTRACT -> switch (parserContext.pollTokenOrThrow("Unterminated 'vx -= ' statement!", vx.getSourcePosition())) {
                case RegisterLiteralToken vy -> List.of(new LeftSubtractRegisterFromRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex()));
                case IntegerLiteralToken n -> {
                    if (n.is8Bits()) {
                        yield List.of(new AddConstantToRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), -n.getValue()));
                    } else {
                        throw new ParserException("Argument '%d' for 'vx -=' does not fit in a byte!".formatted(n.getValue()), n.getSourcePosition());
                    }
                }
                case Token token -> throw new ParserException("Unexpected argument '%s' for 'vx -=' statement".formatted(token.getLexeme()), token.getSourcePosition());
            };
            case SET -> switch (parserContext.pollTokenOrThrow("Unterminated 'vx := ' statement!", vx.getSourcePosition())) {
                case RegisterLiteralToken vy -> List.of(new SetRegisterToRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex()));
                case IntegerLiteralToken n -> {
                    if (n.is8Bits()) {
                        yield List.of(new SetRegisterToConstantAssignment(parserContext.getHere(), vx.getRegisterIndex(), n.getValue()));
                    } else {
                        throw new ParserException("Argument '%d' for 'vx :=' does not fit in a byte!".formatted(n.getValue()), n.getSourcePosition());
                    }
                }
                case AssignmentKeywordToken assignmentKeywordToken -> switch (assignmentKeywordToken.getAssignmentKeyword()) {
                    case DELAY -> List.of(new SetRegisterToDelayTimerAssignment(parserContext.getHere(), vx.getRegisterIndex()));
                    case KEY -> List.of(new SetRegisterToKeyAssignment(parserContext.getHere(), vx.getRegisterIndex()));
                    case RANDOM -> {
                        IntegerLiteralToken nn = parserContext.pollTokenOrThrow(IntegerLiteralToken.class, "Expected literal argument after 'vx := random' assignment!", vx.getSourcePosition());
                        if (nn.is8Bits()) {
                            yield List.of(new SetRegisterToRandomAssignment(parserContext.getHere(), vx.getRegisterIndex(), nn.getValue()));
                        } else {
                            throw new ParserException("Argument '%d' for 'vx := random' does not fit in a byte!".formatted(nn.getValue()), nn.getSourcePosition());
                        }
                    }
                    default -> parserContext.checkReservedName(assignmentKeywordToken);
                };
                case Token token -> throw new ParserException("Unexpected argument '%s' for 'vx :=' statement".formatted(token.getLexeme()), token.getSourcePosition());
            };
        };
    }

    private Collection<CodePrimitive> parseIndexRegister(ParserContext parserContext, IndexRegisterToken indexRegisterToken) throws ParserException {
        return switch (parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operators ':=' or '+=' after an 'i' assignment!", indexRegisterToken.getSourcePosition()).getAssignmentOperation()) {
            case ADD -> List.of(new IncrementIndexRegisterAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' argument after 'i' increment statement!", indexRegisterToken.getSourcePosition()).getRegisterIndex()));
            case SET -> switch (parserContext.pollTokenOrThrow("Expected assignment operators ':=' or '+=' after an 'i' assignment!", indexRegisterToken.getSourcePosition())) {
                case IntegerLiteralToken n -> {
                    if (!n.isUnsigned12Bits()) {
                        throw new ParserException("Argument '%d' for 'i' assignment does not fit in 12 bits!".formatted(n.getValue()), indexRegisterToken.getSourcePosition());
                    }
                    yield List.of(new SetIndexRegisterToConstantAssignment(parserContext.getHere(), new AddressArgument.Resolved(n.getValue())));
                }
                case AssignmentKeywordToken assignmentKeywordToken -> switch (assignmentKeywordToken.getAssignmentKeyword()) {
                    case HEX -> List.of(new SetIndexRegisterToHexCharAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' argument after 'i := hex' increment statement!", indexRegisterToken.getSourcePosition()).getRegisterIndex()));
                    case BIGHEX -> List.of(new SetIndexRegisterToBigHexCharAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' argument after 'i := bighex' increment statement!", indexRegisterToken.getSourcePosition()).getRegisterIndex()));
                    case LONG -> {
                        Token longAssignmentArgumentToken = parserContext.pollTokenOrThrow("Expected 12 bit integer or label argument for 'i := long' statement!", indexRegisterToken.getSourcePosition());
                        if (longAssignmentArgumentToken instanceof IntegerLiteralToken nnnn) {
                            if (!nnnn.isUnsigned16Bits()) {
                                throw new ParserException("Argument '%d' for 'i := long' assignment does not fit in 16 bits!".formatted(nnnn.getValue()), nnnn.getSourcePosition());
                            }
                            yield List.of(new SetIndexRegisterToLongConstantAssignment(parserContext.getHere(), new AddressArgument.Resolved(nnnn.getValue())));
                        } else {
                            parserContext.checkReservedName(longAssignmentArgumentToken);
                            yield List.of(new SetIndexRegisterToLongConstantAssignment(parserContext.getHere(), new AddressArgument.NamedLabelReference(longAssignmentArgumentToken.getLexeme())));
                        }
                    }
                    default -> throw new ParserException("Unexpected name '%s' after 'i' assignment statement!".formatted(indexRegisterToken.getLexeme()), indexRegisterToken.getSourcePosition());
                };
                case Token labelToken -> {
                    parserContext.checkReservedName(labelToken);
                    yield List.of(new SetIndexRegisterToConstantAssignment(parserContext.getHere(), new AddressArgument.NamedLabelReference(labelToken.getLexeme())));
                }
            };
            default -> throw new ParserException("Unexpected operator '%s' after an 'i' assignment!".formatted(indexRegisterToken.getLexeme()), indexRegisterToken.getSourcePosition());
        };
    }

    private Collection<CodePrimitive> parseDirective(ParserContext parserContext, DirectiveToken directiveToken) throws ParserException {
        return switch (directiveToken.getDirective()) {
            case LABEL_DEFINITION -> {
                Token token = parserContext.pollTokenOrThrow("Expected label name following ':' directive!", directiveToken.getSourcePosition());
                parserContext.checkReservedName(token);
                parserContext.addDirectiveDefinition(token, new LabelDefinition(token.getLexeme(), parserContext.getHere()));
                yield List.of();
            }
            case ORG -> List.of();
            case BYTE -> List.of();
            case CALC -> List.of();
            case CALL -> List.of();
            case NEXT -> List.of();
            case ALIAS -> List.of();
            case CONST -> List.of();
            case MACRO -> List.of();
            case PROTO -> List.of();
            case ASSERT -> List.of();
            case UNPACK -> List.of();
            case MONITOR -> List.of();
            case POINTER -> List.of();
            case BREAKPOINT -> List.of();
            case STRING_MODE -> List.of();
        };
    }

    private static class ParserContext {


        private final SourceStream<Token> tokenStream;
        private final List<CodeElement> codeElements = new ArrayList<>();
        private final Map<String, DirectiveDefinition> directiveDefinitions = new HashMap<>();
        private final Map<Integer, Integer> addressedLabelDefinitions = new HashMap<>();
        private final Stack<Integer> loopStack = new Stack<>();
        private int here = 0x200;

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
            return Optional.ofNullable(this.directiveDefinitions.get(token.getLexeme()));
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

        private Map<Integer, Integer> getAddressedLabelDefinitions() {
            return Map.copyOf(this.addressedLabelDefinitions);
        }

        private void addCodeElement(CodeElement codeElement) {
            this.codeElements.add(codeElement);
        }

        private void addDirectiveDefinition(Token token, DirectiveDefinition directiveDefinition) throws ParserException {
            String name = token.getLexeme();
            if (this.directiveDefinitions.containsKey(name)) {
                throw new ParserException("Directive name '%s' is already defined!".formatted(name), token.getSourcePosition());
            }
            checkReservedName(token);
            this.directiveDefinitions.put(name, directiveDefinition);
        }

        private void addAddressedLabelDefinition(int addressKey, int addressValue) {
            if (this.addressedLabelDefinitions.containsKey(addressKey)) {
                throw new IllegalArgumentException("Tried to insert internal label definition with key '%d' that is already mapped to '%d'!".formatted(addressKey, this.addressedLabelDefinitions.get(addressKey)));
            }
            this.addressedLabelDefinitions.put(addressKey, addressValue);
        }

        private void incrementHere(Token token, CodePrimitive codePrimitive) throws ParserException {
            this.incrementHere(token, codePrimitive.getSizeInBytes());
        }

        private int addToHere(Token token, int amount) throws ParserException {
            int newHere = this.here + amount;
            if (newHere > 0xFFFF) {
                throw new ParserException("ROM size exceeds the 16-bit integer limit!", token.getSourcePosition());
            }
            return newHere;
        }

        private void incrementHere(Token token, int amount) throws ParserException {
            this.here = this.addToHere(token, amount);
        }

        private void pushLoop() {
            this.loopStack.push(this.here);
        }

        private OptionalInt peekLoop() {
            return this.loopStack.isEmpty() ? OptionalInt.empty() : OptionalInt.of(this.loopStack.peek());
        }

        private OptionalInt popLoop() {
            return this.loopStack.isEmpty() ? OptionalInt.empty() : OptionalInt.of(this.loopStack.pop());
        }

        private Optional<Token> pollToken() {
            return this.tokenStream.poll()
                    .flatMap(token -> this.getDirective(token)
                            .map(directiveDefinition -> {
                                this.expandDirective(directiveDefinition);
                                return this.pollToken();
                            }).orElse(Optional.of(token)));
        }

        private Token pollTokenOrThrow(String error, SourcePosition sourcePosition) throws ParserException {
            return this.pollToken().orElseThrow(() -> new ParserException(error, sourcePosition));
        }

        @SuppressWarnings("unchecked")
        private <T extends Token> T pollTokenOrThrow(Class<T> tokenClass, String error, SourcePosition sourcePosition) throws ParserException {
            Token token = this.pollTokenOrThrow(error, sourcePosition);
            if (tokenClass.isInstance(token)) {
                return (T) token;
            } else {
                throw new ParserException(error, sourcePosition);
            }
        }

        private Optional<Token> peekToken() {
            return this.tokenStream.peek()
                    .flatMap(token -> this.getDirective(token)
                            .map(directiveDefinition -> {
                                this.expandDirective(directiveDefinition);
                                return this.peekToken();
                            }).orElse(Optional.of(token)));
        }

        private void expandDirective(DirectiveDefinition directiveDefinition) {
            List<Token> directiveTokens = directiveDefinition.expand(this.here);
            for (int i = directiveTokens.size() - 1; i >= 0; i--) {
                this.tokenStream.offerFront(directiveTokens.get(i));
            }
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

        private <T extends Token> Optional<T> peekTokenAndPollIfPresent(Class<T> tokenClass) {
            return this.peekToken(tokenClass).map(token -> {
                this.pollToken();
                return token;
            });
        }

        private Collection<CodePrimitive> checkReservedName(Token token) throws ParserException {
            if (token instanceof ReservedNameToken) {
                throw new ParserException("The name '%s' is reserved and cannot be used as a label!".formatted(token.getLexeme()), token.getSourcePosition());
            } else {
                return List.of(new CallStatement(this.here, new AddressArgument.NamedLabelReference(token.getLexeme())));
            }
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
