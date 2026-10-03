package io.github.arkosammy12.core.parser;

import io.github.arkosammy12.core.codegen.*;
import io.github.arkosammy12.core.grammar.IfBlockKeywordLexeme;
import io.github.arkosammy12.core.grammar.LoopBlockKeywordLexeme;
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
            while (!parserContext.tokenStream.isEmpty()) {
                Optional<Token> optionalToken = parserContext.tokenStream.poll();
                if (optionalToken.isPresent()) {
                    Token token = optionalToken.get();
                    Optional<? extends CodeElement> codeElement = switch (token) {
                        case IfBlockKeywordToken ifBlockKeywordToken -> this.parseIfBlockKeywordToken(parserContext, ifBlockKeywordToken);
                        case LoopBlockKeywordToken loopBlockKeywordToken -> this.parseLoopBlockKeywordToken(parserContext, loopBlockKeywordToken);
                        default -> this.parseTopLevel(parserContext, token);
                    };
                    codeElement.ifPresent(parserContext.codeElements::add);
                }
            }
            return new ParserResult.Ok(List.copyOf(parserContext.codeElements), parserContext.getLabelDefinitions());
        } catch (ParserException e) {
            return e.toErrorResult();
        }
    }

    private Optional<? extends CodeElement> parseTopLevel(ParserContext parserContext, Token token) throws ParserException {
        Optional<CodePrimitive> codePrimitive = this.parseToken(parserContext, token);
        if (codePrimitive.isPresent()) {
            parserContext.incrementHere(token, codePrimitive.get());
        }
        return codePrimitive;
    }

    private Optional<CodeElement> parseIfBlockKeywordToken(ParserContext parserContext, IfBlockKeywordToken ifBlockKeywordToken) throws ParserException {
        if (ifBlockKeywordToken.getIfBlockKeywordLexeme() != IfBlockKeywordLexeme.IF) {
            throw new ParserException("Unmatched '%s' if block keyword!".formatted(ifBlockKeywordToken.getIfBlockKeywordLexeme()), ifBlockKeywordToken.getSourcePosition());
        }
        // HERE is pointing to the first opcode of the conditional expression
        int firstConditionalOpcodeOffset = parserContext.here;
        List<CodePrimitive> conditionalExpressionOpcodes = this.resolveConditionalExpression(parserContext, ifBlockKeywordToken);

        IfBlockKeywordToken ifBlockBeginningToken = this.pollTokenOrThrow(IfBlockKeywordToken.class, parserContext, "Unexpected if block beginning!", ifBlockKeywordToken.getSourcePosition());
        return switch (ifBlockBeginningToken.getIfBlockKeywordLexeme()) {
            case THEN -> {
                // HERE is pointing to the skipped instruction, which is surrounded by the if-then block
                CodeElement ifThenBlockElement = null;
                Token ifThenToken = null;
                while (ifThenBlockElement == null && !parserContext.tokenStream.isEmpty()) {
                    Optional<Token> optionalToken = this.pollToken(parserContext);
                    if (optionalToken.isPresent()) {
                        ifThenToken = optionalToken.get();
                        ifThenBlockElement = (switch (ifThenToken) {
                            case IfBlockKeywordToken innerIfBlockKeyword -> this.parseIfBlockKeywordToken(parserContext, innerIfBlockKeyword);
                            case LoopBlockKeywordToken innerLoopBlockKeyword -> this.parseLoopBlockKeywordToken(parserContext, innerLoopBlockKeyword);
                            case Token innerToken -> this.parseToken(parserContext, innerToken);
                        }).orElse(null);
                    }
                }
                if (ifThenBlockElement instanceof CodePrimitive ifThenBlockCodePrimitive) {
                    parserContext.incrementHere(ifThenToken, ifThenBlockCodePrimitive);
                }
                // HERE is pointing to the instruction after the skipped instruction, ending the if-then block
                yield Optional.of(new IfThenBlock(firstConditionalOpcodeOffset, conditionalExpressionOpcodes, ifThenBlockElement));
            }
            case BEGIN -> {
                // HERE is pointing to the jump instruction that will jump to the end if the if-begin block or to the
                // else case of the if-else block

                // Invert the skip instruction since now the skip instruction will surround the jump instruction that
                // prevents the if block from being executed, and not the execution of the if block itself
                conditionalExpressionOpcodes = this.invertSkips(conditionalExpressionOpcodes);

                LabelableInstruction jumpAboveIfBlockStatement = new JumpStatement(parserContext.here, new AddressArgument.LabelReference("if-block-initial-jump"));
                parserContext.incrementHere(ifBlockBeginningToken, (JumpStatement) jumpAboveIfBlockStatement);

                // HERE now points to the first instruction of the if block within the if-begin or if-else block
                // From now on we are looking for an 'end' or an 'else' followed by an 'end'

                List<CodeElement> ifBlockElements = new ArrayList<>();
                List<CodeElement> elseBlockElements = null;
                LabelableInstruction jumpAboveElseBlockStatement = null;

                while (!parserContext.tokenStream.isEmpty()) {
                    Optional<Token> optionalToken = this.pollToken(parserContext);
                    if (optionalToken.isPresent()) {
                        Token token = optionalToken.get();
                        Optional<? extends CodeElement> optionalCodeElement = Optional.empty();
                        if (token instanceof IfBlockKeywordToken innerIfBlockKeyword) {
                            switch (innerIfBlockKeyword.getIfBlockKeywordLexeme()) {
                                case ELSE -> {
                                    // HERE is pointing to the jump instruction at the end of the if block,
                                    // that jumps to after the else block
                                    jumpAboveElseBlockStatement = new JumpStatement(parserContext.here, new AddressArgument.LabelReference("if-block-end-jump-statement"));
                                    parserContext.incrementHere(token, (JumpStatement) jumpAboveElseBlockStatement);

                                    // HERE now points to the first instruction of the else block
                                    jumpAboveIfBlockStatement = jumpAboveIfBlockStatement.resolve(parserContext.here);

                                    elseBlockElements = new ArrayList<>();

                                    // Just continue to the next iteration now that we've consumed the 'else' token
                                    continue;
                                }
                                case END -> {
                                    // HERE is pointing to the first instruction after the end if the if-begin or if-else block
                                    if (elseBlockElements == null) {
                                        jumpAboveIfBlockStatement = jumpAboveIfBlockStatement.resolve(parserContext.here);
                                        yield Optional.of(new IfBeginEndBlock(firstConditionalOpcodeOffset, conditionalExpressionOpcodes, ifBlockElements, (JumpStatement) jumpAboveIfBlockStatement));
                                    } else {
                                        // Resolve the jump statement at the end of the if block and before the else block,
                                        // which jumps over the else block
                                        jumpAboveElseBlockStatement = jumpAboveElseBlockStatement.resolve(parserContext.here);
                                        yield Optional.of(new IfElseBlock(firstConditionalOpcodeOffset, conditionalExpressionOpcodes, ifBlockElements, (JumpStatement) jumpAboveIfBlockStatement, elseBlockElements, (JumpStatement) jumpAboveElseBlockStatement));
                                    }
                                }
                                default -> optionalCodeElement = this.parseIfBlockKeywordToken(parserContext, innerIfBlockKeyword);
                            }
                        } else {
                            optionalCodeElement = switch (token) {
                                case LoopBlockKeywordToken innerLoopBlockKeyword -> this.parseLoopBlockKeywordToken(parserContext, innerLoopBlockKeyword);
                                case Token innerToken -> this.parseToken(parserContext, innerToken);
                            };
                        }
                        if (optionalCodeElement.isPresent()) {
                            CodeElement codeElement = optionalCodeElement.get();
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

    private Optional<CodeElement> parseLoopBlockKeywordToken(ParserContext parserContext, LoopBlockKeywordToken loopBlockKeywordToken) throws ParserException {
        // Plan. Fail on encountering a 'while` after a 'then'.
        // In here we handle emitting a jump with a placer holder after encountering an 'again',
        // which will be filled in by the expansion of the loop block during codegen.
        // We will handle a 'while' similarly within an if-begin or if-else block in the method above
        // In here we will handle constructing the loop block and patching the backwards jump corresponding to the
        // matching 'again' and emitting placeholder forward jumps for top level 'while' statements.

        if (loopBlockKeywordToken.getLoopBlockKeyword() != LoopBlockKeywordLexeme.LOOP) {

         }

        return Optional.empty();
    }

    private List<CodePrimitive> invertSkips(List<CodePrimitive> codePrimitives) {
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

    private List<CodePrimitive> resolveConditionalExpression(ParserContext parserContext, Token token) throws ParserException {
        RegisterLiteralToken vx = this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected vx in conditional expression!", token.getSourcePosition());
        ConditionalOperatorToken conditionalOperatorToken = this.pollTokenOrThrow(ConditionalOperatorToken.class, parserContext, "Expected conditional operator in conditional statement!", vx.getSourcePosition());
        ConditionalOperation conditionalOperation = conditionalOperatorToken.getConditionalOperation();
        return switch (conditionalOperation) {
            case KEY_PRESSED -> {
                CodePrimitive keyNotPressed = new SkipIfKeyNotPressed(parserContext.here, vx.getRegisterIndex());
                parserContext.incrementHere(conditionalOperatorToken, keyNotPressed);
                yield List.of(keyNotPressed);
            }
            case KEY_NOT_PRESSED -> {
                CodePrimitive keyPressed = new SkipIfKeyPressed(parserContext.here, vx.getRegisterIndex());
                parserContext.incrementHere(conditionalOperatorToken, keyPressed);
                yield List.of(keyPressed);
            }
            default -> {
                Token argumentToken = this.pollTokenOrThrow(parserContext, "Expected vy or NN argument in conditional expression!", conditionalOperatorToken.getSourcePosition());
                yield switch (argumentToken) {
                    case RegisterLiteralToken vy -> switch (conditionalOperation) {
                        case EQUALITY -> {
                            CodePrimitive skipIfRegistersNotEqual = new SkipIfRegistersNotEqual(parserContext.here, vx.getRegisterIndex(), vy.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, skipIfRegistersNotEqual);
                            yield List.of(skipIfRegistersNotEqual);
                        }
                        case INEQUALITY -> {
                            CodePrimitive skipIfRegistersEqual = new SkipIfRegistersEqual(parserContext.here, vx.getRegisterIndex(), vy.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, skipIfRegistersEqual);
                            yield List.of(skipIfRegistersEqual);
                        }
                        case GREATER_THAN -> {
                            CodePrimitive assign = new SetRegisterToRegisterAssignment(parserContext.here, 0xF,  vy.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, assign);

                            CodePrimitive subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.here, 0xF, vx.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, subtract);

                            CodePrimitive skip = new SkipIfRegisterNotEqualsConstant(parserContext.here, 0xF, 0x00);
                            parserContext.incrementHere(argumentToken, skip);

                            yield List.of(assign, subtract, skip);
                        }
                        case GREATER_THAN_OR_EQUALS -> {
                            CodePrimitive assign = new SetRegisterToRegisterAssignment(parserContext.here, 0xF,  vy.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, assign);

                            CodePrimitive subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.here, 0xF, vx.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, subtract);

                            CodePrimitive skip = new SkipIfRegisterEqualsConstant(parserContext.here, 0xF, 0x00);
                            parserContext.incrementHere(argumentToken, skip);

                            yield List.of(assign, subtract, skip);
                        }
                        case LESS_THAN -> {
                            CodePrimitive assign = new SetRegisterToRegisterAssignment(parserContext.here, 0xF,  vy.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, assign);

                            CodePrimitive subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.here, 0xF, vx.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, subtract);

                            CodePrimitive skip = new SkipIfRegisterNotEqualsConstant(parserContext.here, 0xF, 0x00);
                            parserContext.incrementHere(argumentToken, skip);

                            yield List.of(assign, subtract, skip);
                        }
                        case LESS_THAN_OR_EQUALS -> {
                            CodePrimitive assign = new SetRegisterToRegisterAssignment(parserContext.here, 0xF,  vy.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, assign);

                            CodePrimitive subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.here, 0xF, vx.getRegisterIndex());
                            parserContext.incrementHere(argumentToken, subtract);

                            CodePrimitive skip = new SkipIfRegisterEqualsConstant(parserContext.here, 0xF, 0x00);
                            parserContext.incrementHere(argumentToken, skip);

                            yield List.of(assign, subtract, skip);
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + conditionalOperation);
                    };
                    case IntegerLiteralToken nn -> {
                        if (!nn.is8Bits()) {
                            throw new ParserException("Conditional argument '%d' does not fit in 8 bits [-128, 255]".formatted(nn.getValue()), nn.getSourcePosition());
                        }
                        yield switch (conditionalOperation) {
                            case EQUALITY -> {
                                CodePrimitive skipIfRegisterNotEqualConstant = new SkipIfRegisterNotEqualsConstant(parserContext.here, vx.getRegisterIndex(), nn.getValue());
                                parserContext.incrementHere(argumentToken, skipIfRegisterNotEqualConstant);
                                yield List.of(skipIfRegisterNotEqualConstant);
                            }
                            case INEQUALITY -> {
                                CodePrimitive skipIfRegisterEqualsConstant = new SkipIfRegisterEqualsConstant(parserContext.here, vx.getRegisterIndex(), nn.getValue());
                                parserContext.incrementHere(argumentToken, skipIfRegisterEqualsConstant);
                                yield List.of(skipIfRegisterEqualsConstant);
                            }
                            case GREATER_THAN -> {
                                CodePrimitive assign = new SetRegisterToConstantAssignment(parserContext.here, 0xF,  nn.getValue());
                                parserContext.incrementHere(argumentToken, assign);

                                CodePrimitive subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.here, 0xF, vx.getRegisterIndex());
                                parserContext.incrementHere(argumentToken, subtract);

                                CodePrimitive skip = new SkipIfRegisterNotEqualsConstant(parserContext.here, 0xF, 0x00);
                                parserContext.incrementHere(argumentToken, skip);

                                yield List.of(assign, subtract, skip);
                            }
                            case GREATER_THAN_OR_EQUALS -> {
                                CodePrimitive assign = new SetRegisterToConstantAssignment(parserContext.here, 0xF,  nn.getValue());
                                parserContext.incrementHere(argumentToken, assign);

                                CodePrimitive subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.here, 0xF, vx.getRegisterIndex());
                                parserContext.incrementHere(argumentToken, subtract);

                                CodePrimitive skip = new SkipIfRegisterEqualsConstant(parserContext.here, 0xF, 0x00);
                                parserContext.incrementHere(argumentToken, skip);

                                yield List.of(assign, subtract, skip);
                            }
                            case LESS_THAN -> {
                                CodePrimitive assign = new SetRegisterToConstantAssignment(parserContext.here, 0xF,  nn.getValue());
                                parserContext.incrementHere(argumentToken, assign);

                                CodePrimitive subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.here, 0xF, vx.getRegisterIndex());
                                parserContext.incrementHere(argumentToken, subtract);

                                CodePrimitive skip = new SkipIfRegisterNotEqualsConstant(parserContext.here, 0xF, 0x00);
                                parserContext.incrementHere(argumentToken, skip);

                                yield List.of(assign, subtract, skip);
                            }
                            case LESS_THAN_OR_EQUALS -> {
                                CodePrimitive assign = new SetRegisterToConstantAssignment(parserContext.here, 0xF,  nn.getValue());
                                parserContext.incrementHere(argumentToken, assign);

                                CodePrimitive subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.here, 0xF, vx.getRegisterIndex());
                                parserContext.incrementHere(argumentToken, subtract);

                                CodePrimitive skip = new SkipIfRegisterEqualsConstant(parserContext.here, 0xF, 0x00);
                                parserContext.incrementHere(argumentToken, skip);

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

    private Optional<CodePrimitive> parseToken(ParserContext parserContext, Token token) throws ParserException  {
        return switch (token) {
            case DirectiveToken directiveToken -> this.parseDirective(parserContext, directiveToken);
            case InstructionStatementNameToken instructionStatementNameToken -> this.parseInstructionStatementNameToken(parserContext, instructionStatementNameToken);
            case LiteralToken literalToken -> this.parseLiteral(parserContext, literalToken);
            case IndexRegisterToken indexRegisterToken -> this.parseIndexRegister(parserContext, indexRegisterToken);
            case AssignmentKeywordToken assignmentKeywordToken -> this.parseAssignmentKeywordToken(parserContext, assignmentKeywordToken);
            default -> this.checkReservedName(parserContext, token);
        };
    }

    private Optional<CodePrimitive> parseInstructionStatementNameToken(ParserContext parserContext, InstructionStatementNameToken instructionStatementNameToken) throws ParserException {
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
    
    private Optional<CodePrimitive> parseLiteral(ParserContext parserContext, LiteralToken literalToken) throws ParserException {
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

    private Optional<CodePrimitive> parseAssignmentKeywordToken(ParserContext parserContext, AssignmentKeywordToken assignmentKeywordToken) throws ParserException {
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
    
    private Optional<CodePrimitive> parseRegisterLiteral(ParserContext parserContext, RegisterLiteralToken vx) throws ParserException {
        return switch (this.pollTokenOrThrow(AssignmentOperatorToken.class, parserContext, "Unknown operator on register assignment statement!", vx.getSourcePosition()).getAssignmentOperation()) {
            case BITWISE_OR -> Optional.of(new BitwiseOrRegisterAssignment(parserContext.here, vx.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected vy argument after 'vx |=' assignment!", vx.getSourcePosition()).getRegisterIndex()));
            case BITWISE_AND -> Optional.of(new BitwiseAndRegisterAssignment(parserContext.here, vx.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected vy argument after 'vx &=' assignment!", vx.getSourcePosition()).getRegisterIndex()));
            case BITWISE_XOR -> Optional.of(new BitwiseXorRegisterAssignment(parserContext.here, vx.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected vy argument after 'vx ^=' assignment!", vx.getSourcePosition()).getRegisterIndex()));
            case RIGHT_SHIFT -> Optional.of(new RightShiftRegisterAssignment(parserContext.here, vx.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected vy argument after 'vx >>=' assignment!", vx.getSourcePosition()).getRegisterIndex()));
            case LEFT_SHIFT -> Optional.of(new LeftShiftRegisterAssignment(parserContext.here, vx.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected vy argument after 'vx <<=' assignment!", vx.getSourcePosition()).getRegisterIndex()));
            case RIGHT_SUBTRACT -> Optional.of(new RightSubtractRegisterFromRegisterAssignment(parserContext.here, vx.getRegisterIndex(), this.pollTokenOrThrow(RegisterLiteralToken.class, parserContext, "Expected vy argument after 'vx =-' assignment!", vx.getSourcePosition()).getRegisterIndex()));
            case ADD -> switch (this.pollTokenOrThrow(parserContext, "Unterminated 'vx += ' statement!", vx.getSourcePosition())) {
                case RegisterLiteralToken vy -> Optional.of(new AddRegisterToRegisterAssignment(parserContext.here, vx.getRegisterIndex(), vy.getRegisterIndex()));
                case IntegerLiteralToken n -> {
                    if (n.is8Bits()) {
                        yield Optional.of(new AddConstantToRegisterAssignment(parserContext.here, vx.getRegisterIndex(), n.getValue()));
                    } else {
                        throw new ParserException("Argument '%d' for 'vx +=' does not fit in a byte!".formatted(n.getValue()), n.getSourcePosition());
                    }
                }
                case Token token -> throw new ParserException("Unexpected argument '%s' for 'vx +=' statement".formatted(token.getLexeme()), token.getSourcePosition());
            };
            case LEFT_SUBTRACT -> switch (this.pollTokenOrThrow(parserContext, "Unterminated 'vx -= ' statement!", vx.getSourcePosition())) {
                case RegisterLiteralToken vy -> Optional.of(new LeftSubtractRegisterFromRegisterAssignment(parserContext.here, vx.getRegisterIndex(), vy.getRegisterIndex()));
                case IntegerLiteralToken n -> {
                    if (n.is8Bits()) {
                        yield Optional.of(new AddConstantToRegisterAssignment(parserContext.here, vx.getRegisterIndex(), -n.getValue()));
                    } else {
                        throw new ParserException("Argument '%d' for 'vx -=' does not fit in a byte!".formatted(n.getValue()), n.getSourcePosition());
                    }
                }
                case Token token -> throw new ParserException("Unexpected argument '%s' for 'vx -=' statement".formatted(token.getLexeme()), token.getSourcePosition());
            };
            case SET -> switch (this.pollTokenOrThrow(parserContext, "Unterminated 'vx := ' statement!", vx.getSourcePosition())) {
                case RegisterLiteralToken vy -> Optional.of(new SetRegisterToRegisterAssignment(parserContext.here, vx.getRegisterIndex(), vy.getRegisterIndex()));
                case IntegerLiteralToken n -> {
                    if (n.is8Bits()) {
                        yield Optional.of(new SetRegisterToConstantAssignment(parserContext.here, vx.getRegisterIndex(), n.getValue()));
                    } else {
                        throw new ParserException("Argument '%d' for 'vx :=' does not fit in a byte!".formatted(n.getValue()), n.getSourcePosition());
                    }
                }
                case AssignmentKeywordToken assignmentKeywordToken -> switch (assignmentKeywordToken.getAssignmentKeyword()) {
                    case DELAY -> Optional.of(new SetRegisterToDelayTimerAssignment(parserContext.here, vx.getRegisterIndex()));
                    case KEY -> Optional.of(new SetRegisterToKeyAssignment(parserContext.here, vx.getRegisterIndex()));
                    case RANDOM -> {
                        IntegerLiteralToken nn = this.pollTokenOrThrow(IntegerLiteralToken.class, parserContext, "Expected literal argument after 'vx := random' assignment!", vx.getSourcePosition());
                        if (nn.is8Bits()) {
                            yield Optional.of(new SetRegisterToRandomAssignment(parserContext.here, vx.getRegisterIndex(), nn.getValue()));
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

    private Optional<CodePrimitive> parseIndexRegister(ParserContext parserContext, IndexRegisterToken indexRegisterToken) throws ParserException {
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

    private Optional<CodePrimitive> parseDirective(ParserContext parserContext, DirectiveToken directiveToken) throws ParserException {
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

    private Optional<CodePrimitive> checkReservedName(ParserContext parserContext, Token token) throws ParserException {
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

        private void incrementHere(Token token, CodePrimitive codePrimitive) throws ParserException {
            this.incrementHere(token, codePrimitive.getSizeInBytes());
        }

        private void incrementHere(Token token, int amount) throws ParserException {
            int newHere = this.here + amount;
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
