package io.github.arkosammy12.core.parser;

import io.github.arkosammy12.core.elements.*;
import io.github.arkosammy12.core.result.OctoAssemblerException;
import io.github.arkosammy12.core.grammar.IfBlockKeywordLexeme;
import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.lexer.SourceStream;
import io.github.arkosammy12.core.parser.directive.DirectiveDefinition;
import io.github.arkosammy12.core.parser.directive.ExpandableDirective;
import io.github.arkosammy12.core.parser.directive.LabelDefinition;
import io.github.arkosammy12.core.result.OctoParserResult;
import io.github.arkosammy12.core.token.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

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
                    for (CodeElement codeElement : codeElements) {
                        parserContext.addCodeElement(codeElement);
                    }
                }
            }
            if (parserContext.foundMainLabel()) {
                return new OctoParserResult.Ok(parserContext.getCodeElements(), parserContext.getLabelDefinitions(), parserContext.getInternalLabelDefinitions());
            } else {
                return new OctoParserResult.Error("This program is missing a 'main' label!");
            }
        } catch (OctoAssemblerException e) {
            return e.toParserResult();
        }
    }

    private Collection<? extends CodeElement> parseTopLevel(ParserContext parserContext, Token token) throws OctoAssemblerException {
        Collection<CodePrimitive> codePrimitives = this.parseToken(parserContext, token);
        for (CodePrimitive codePrimitive : codePrimitives) {
            parserContext.incrementHere(token, codePrimitive);
        }
        return codePrimitives;
    }

    private Collection<CodeElement> parseIfBlockKeywordToken(ParserContext parserContext, IfBlockKeywordToken ifBlockKeywordToken) throws OctoAssemblerException {
        if (ifBlockKeywordToken.getIfBlockKeywordLexeme() != IfBlockKeywordLexeme.IF) {
            throw new OctoAssemblerException("Invalid start of if block '%s'!".formatted(ifBlockKeywordToken.getIfBlockKeywordLexeme()), ifBlockKeywordToken.getSourcePosition());
        }

        // HERE is pointing to the first opcode of the conditional expression
        Collection<CodePrimitive> conditionalExpressionOpcodes = this.parseConditionalExpression(parserContext, ifBlockKeywordToken);
        for (CodePrimitive conditionalExpressionOpcode : conditionalExpressionOpcodes) {
            parserContext.incrementHere(ifBlockKeywordToken, conditionalExpressionOpcode);
        }

        IfBlockKeywordToken ifBlockBeginningToken = parserContext.pollTokenOrThrow(IfBlockKeywordToken.class, "Expected 'begin' or 'then' after if block conditional expression!", ifBlockKeywordToken.getSourcePosition(), token -> "Invalid if block beginning keyword '%s'!".formatted(token.getLexeme()));
        return switch (ifBlockBeginningToken.getIfBlockKeywordLexeme()) {
            case THEN -> {
                // HERE is pointing to the skipped instruction, which is surrounded by the if-then block
                List<CodeElement> ifThenBlockElements = new ArrayList<>();
                Token ifThenBlockToken = null;
                while (ifThenBlockElements.isEmpty() && !parserContext.getTokenStream().isEmpty()) {
                    Optional<Token> optionalToken = parserContext.pollToken();
                    if (optionalToken.isPresent()) {
                        ifThenBlockToken = optionalToken.get();
                        ifThenBlockElements.addAll(switch (ifThenBlockToken) {
                            case IfBlockKeywordToken innerIfBlockKeyword -> this.parseIfBlockKeywordToken(parserContext, innerIfBlockKeyword);
                            case LoopBlockKeywordToken innerLoopBlockKeywordToken -> this.parseLoopBlockKeywordToken(parserContext, innerLoopBlockKeywordToken);
                            case Token innerToken -> this.parseToken(parserContext, innerToken);
                        });
                    }
                }

                if (ifThenBlockElements.isEmpty()) {
                    yield List.of(new IfThenBlock(conditionalExpressionOpcodes));
                } else {
                    CodeElement ifThenBlockElement = ifThenBlockElements.getFirst();
                    if (ifThenBlockElement instanceof CodePrimitive codePrimitive) {
                        parserContext.incrementHere(ifThenBlockToken, codePrimitive);
                    }

                    // HERE is pointing to the instruction after the skipped instruction, ending the if-then block
                    Collection<CodeElement> extraCodeElements = new ArrayList<>();
                    extraCodeElements.add(new IfThenBlock(conditionalExpressionOpcodes, ifThenBlockElement));
                    for (int i = 1; i < ifThenBlockElements.size(); i++) {
                        CodeElement extraCodeElement = ifThenBlockElements.get(i);
                        if (extraCodeElement instanceof CodePrimitive codePrimitive) {
                            parserContext.incrementHere(ifThenBlockToken, codePrimitive);
                        }
                        extraCodeElements.add(extraCodeElement);
                    }

                    yield extraCodeElements;
                }
            }
            case BEGIN -> {
                // Invert the skip instruction since now the skip instruction will surround the jump instruction that
                // prevents the if block from being executed, and not the execution of the if block itself
                conditionalExpressionOpcodes = this.invertSkipConditions(conditionalExpressionOpcodes);

                // HERE is pointing to the jump instruction that will jump to the end of the if-begin block or to the
                // else case of the if-else block
                JumpStatement jumpAboveIfBlockStatement = new JumpStatement(parserContext.getHere(), new AddressArgument.InternalLabelReference(new InternalLabelKey(parserContext.getHere(), ifBlockBeginningToken.getSourcePosition())));
                parserContext.incrementHere(ifBlockBeginningToken, jumpAboveIfBlockStatement);

                // HERE now points to the first instruction of the if block within the if-begin or if-else block.
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
                                    // HERE is pointing to the jump instruction at the end of the if block, that jumps
                                    // to after the else block
                                    jumpAboveElseBlockStatement = new JumpStatement(parserContext.getHere(), new AddressArgument.InternalLabelReference(new InternalLabelKey(parserContext.getHere(), ifBlockBeginningToken.getSourcePosition())));
                                    parserContext.incrementHere(token, jumpAboveElseBlockStatement);

                                    // HERE now points to the first instruction of the else block
                                    jumpAboveIfBlockStatement.resolve(parserContext.getHere());

                                    // Initialize our list of 'else' block elements
                                    elseBlockElements = new ArrayList<>();

                                    // Just continue to the next iteration now that we've consumed the 'else' token
                                    continue;
                                }
                                case END -> {
                                    // HERE is pointing to the first instruction after the end of the if-begin or if-else block
                                    if (elseBlockElements == null) {
                                        jumpAboveIfBlockStatement.resolve(parserContext.getHere());
                                        yield List.of(new IfBeginEndBlock(conditionalExpressionOpcodes, jumpAboveIfBlockStatement, ifBlockElements));
                                    } else {
                                        // Resolve the jump statement at the end of the if block and before the else block,
                                        // which jumps over the else block
                                        jumpAboveElseBlockStatement.resolve(parserContext.getHere());
                                        yield List.of(new IfElseBlock(conditionalExpressionOpcodes, jumpAboveIfBlockStatement, ifBlockElements, jumpAboveElseBlockStatement, elseBlockElements));
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
                throw new OctoAssemblerException("Unterminated if block!", ifBlockKeywordToken.getSourcePosition());
            }
            default -> throw new OctoAssemblerException("Unexpected if block beginning!", ifBlockKeywordToken.getSourcePosition());
        };
    }

    private Collection<CodePrimitive> parseLoopBlockKeywordToken(ParserContext parserContext, LoopBlockKeywordToken loopBlockKeywordToken) throws OctoAssemblerException {
        return switch (loopBlockKeywordToken.getLoopBlockKeyword()) {
            case LOOP -> {
                parserContext.pushLoop(loopBlockKeywordToken.getSourcePosition());
                yield List.of();
            }
            case AGAIN -> {
                Optional<InternalLabelKey> optionalLoopKey = parserContext.popLoop();
                if (optionalLoopKey.isEmpty()) {
                    throw new OctoAssemblerException("This 'again' does not have a matching 'loop'!", loopBlockKeywordToken.getSourcePosition());
                } else {
                    InternalLabelKey loopKey = optionalLoopKey.get();
                    JumpStatement jumpToLoopBeginningStatement = new JumpStatement(parserContext.getHere(), new AddressArgument.Resolved(loopKey.addressKey()));
                    parserContext.addInternalLabelDefinition(loopKey, parserContext.addToHere(loopBlockKeywordToken, jumpToLoopBeginningStatement.getSizeInBytes()));
                    yield List.of(jumpToLoopBeginningStatement);
                }
            }
            case WHILE -> {
                Optional<InternalLabelKey> loopKey = parserContext.peekLoop();
                if (loopKey.isEmpty()) {
                    throw new OctoAssemblerException("This 'while' is not within a 'loop'!", loopBlockKeywordToken.getSourcePosition());
                } else {
                    Collection<CodePrimitive> conditionalExpressionOpcodes = this.invertSkipConditions(this.parseConditionalExpression(parserContext, loopBlockKeywordToken));
                    JumpStatement jumpToLoopEndStatement = new JumpStatement(parserContext.addToHere(loopBlockKeywordToken, conditionalExpressionOpcodes.stream().map(CodePrimitive::getSizeInBytes).reduce(Integer::sum).orElse(0)), new AddressArgument.InternalLabelReference(loopKey.get()));
                    List<CodePrimitive> whileStatementElements = new ArrayList<>(conditionalExpressionOpcodes);
                    whileStatementElements.add(jumpToLoopEndStatement);
                    yield whileStatementElements;
                }
            }
        };
    }

    private Collection<CodePrimitive> parseConditionalExpression(ParserContext parserContext, Token token) throws OctoAssemblerException {
        RegisterLiteralToken vx = parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' operand in conditional expression!", token.getSourcePosition(), unmatchedToken -> "The operand '%s' is not a register!".formatted(unmatchedToken.getLexeme()));
        ConditionalOperatorToken conditionalOperatorToken = parserContext.pollTokenOrThrow(ConditionalOperatorToken.class, "Expected conditional operator in conditional statement!", vx.getSourcePosition(), unmatchedToken -> "Unknown conditional operator '%s'!".formatted(unmatchedToken.getLexeme()));
        ConditionalOperation conditionalOperation = conditionalOperatorToken.getConditionalOperation();
        return switch (conditionalOperation) {
            case KEY_PRESSED -> List.of(new SkipIfKeyNotPressed(parserContext.getHere(), vx.getRegisterIndex()));
            case KEY_NOT_PRESSED -> List.of(new SkipIfKeyPressed(parserContext.getHere(), vx.getRegisterIndex()));
            default -> {
                Token argumentToken = parserContext.pollTokenOrThrow("Expected 'vy' or byte argument in conditional expression!", conditionalOperatorToken.getSourcePosition());
                yield switch (argumentToken) {
                    case RegisterLiteralToken vy -> switch (conditionalOperation) {
                        case EQUALITY -> List.of(new SkipIfRegistersNotEqual(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex()));
                        case INEQUALITY -> List.of(new SkipIfRegistersEqual(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex()));
                        case GREATER_THAN -> {
                            CodePrimitive assign = new SetRegisterToRegisterAssignment(parserContext.getHere(), 0xF, vy.getRegisterIndex());
                            CodePrimitive subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.addToHere(argumentToken, assign.getSizeInBytes()), 0xF, vx.getRegisterIndex());
                            CodePrimitive skip = new SkipIfRegisterNotEqualsConstant(parserContext.addToHere(argumentToken, assign.getSizeInBytes() + subtract.getSizeInBytes()), 0xF, 0x00);
                            yield List.of(assign, subtract, skip);
                        }
                        case GREATER_THAN_OR_EQUALS -> {
                            CodePrimitive assign = new SetRegisterToRegisterAssignment(parserContext.getHere(), 0xF, vy.getRegisterIndex());
                            CodePrimitive subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.addToHere(argumentToken, assign.getSizeInBytes()), 0xF, vx.getRegisterIndex());
                            CodePrimitive skip = new SkipIfRegisterEqualsConstant(parserContext.addToHere(argumentToken, assign.getSizeInBytes() + subtract.getSizeInBytes()), 0xF, 0x00);
                            yield List.of(assign, subtract, skip);
                        }
                        case LESS_THAN -> {
                            CodePrimitive assign = new SetRegisterToRegisterAssignment(parserContext.getHere(), 0xF, vy.getRegisterIndex());
                            CodePrimitive subtract = new RightSubtractRegisterFromRegisterAssignment(parserContext.addToHere(argumentToken, assign.getSizeInBytes()), 0xF, vx.getRegisterIndex());
                            CodePrimitive skip = new SkipIfRegisterNotEqualsConstant(parserContext.addToHere(argumentToken, assign.getSizeInBytes() + subtract.getSizeInBytes()), 0xF, 0x00);
                            yield List.of(assign, subtract, skip);
                        }
                        case LESS_THAN_OR_EQUALS -> {
                            CodePrimitive assign = new SetRegisterToRegisterAssignment(parserContext.getHere(), 0xF, vy.getRegisterIndex());
                            CodePrimitive subtract = new LeftSubtractRegisterFromRegisterAssignment(parserContext.addToHere(argumentToken, assign.getSizeInBytes()), 0xF, vx.getRegisterIndex());
                            CodePrimitive skip = new SkipIfRegisterEqualsConstant(parserContext.addToHere(argumentToken, assign.getSizeInBytes() + subtract.getSizeInBytes()), 0xF, 0x00);
                            yield List.of(assign, subtract, skip);
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + conditionalOperation);
                    };
                    case IntegerLiteralToken nn -> {
                        if (!nn.is8Bits()) {
                            throw new OctoAssemblerException("Conditional operand '%d' does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(nn.getValue()), nn.getSourcePosition());
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
                    case Token t -> throw new OctoAssemblerException("Invalid conditional expression operand '%s'!".formatted(t.getLexeme()), t.getSourcePosition());
                };
            }
        };
    }

    private Collection<CodePrimitive> parseToken(ParserContext parserContext, Token token) throws OctoAssemblerException {
        return switch (token) {
            case DirectiveToken directiveToken -> this.parseDirective(parserContext, directiveToken);
            case InstructionStatementNameToken instructionStatementNameToken -> this.parseInstructionStatementNameToken(parserContext, instructionStatementNameToken);
            case LiteralToken literalToken -> this.parseLiteral(parserContext, literalToken);
            case IndexRegisterToken indexRegisterToken -> this.parseIndexRegister(parserContext, indexRegisterToken);
            case AssignmentKeywordToken assignmentKeywordToken -> this.parseAssignmentKeywordToken(parserContext, assignmentKeywordToken);
            default -> parserContext.checkReservedName(token);
        };
    }

    private Collection<CodePrimitive> parseInstructionStatementNameToken(ParserContext parserContext, InstructionStatementNameToken instructionStatementNameToken) throws OctoAssemblerException {
        return switch (instructionStatementNameToken) {
            case SemicolonToken _ -> List.of(new ReturnStatement(parserContext.getHere()));
            case NonSymbolInstructionStatementKeywordToken nonSymbolInstructionStatementKeywordToken ->
                switch (nonSymbolInstructionStatementKeywordToken.getInstructionStatementKeyword()) {
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
                        RegisterLiteralToken vx = parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' operand in 'sprite' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme()));
                        RegisterLiteralToken vy = parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' operand in 'sprite' statement!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme()));
                        IntegerLiteralToken n = parserContext.pollTokenOrThrow(IntegerLiteralToken.class, "Expected 'n' operand in 'sprite' statement!", vy.getSourcePosition(), token -> "The operand '%s' is not an integer!".formatted(token.getLexeme()));
                        if (n.isUnsigned4Bits()) {
                            yield List.of(new SpriteStatement(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex(), n.getValue()));
                        } else {
                            throw new OctoAssemblerException("The 'sprite' statement operand '%d' for 'n' does not fit in 4 bits. Must be in the range [0, 15]!".formatted(n.getValue()), n.getSourcePosition());
                        }
                    }
                    case JUMP -> {
                        Token token = parserContext.pollTokenOrThrow("Expected integer or label operand in 'jump' statement!", instructionStatementNameToken.getSourcePosition());
                        if (token instanceof IntegerLiteralToken n) {
                            if (n.isUnsigned12Bits()) {
                                yield List.of(new JumpStatement(parserContext.getHere(), new AddressArgument.Resolved(n.getValue())));
                            } else {
                                throw new OctoAssemblerException("The 'jump' statement target '%d' does not fit in 12 bits!".formatted(n.getValue()), n.getSourcePosition());
                            }
                        } else {
                            yield List.of(new JumpStatement(parserContext.getHere(), new AddressArgument.NamedLabelReference(token)));
                        }
                    }
                    case JUMP0 -> {
                        Token token = parserContext.pollTokenOrThrow("Expected integer or label operand in 'jump0' statement!", instructionStatementNameToken.getSourcePosition());
                        if (token instanceof IntegerLiteralToken n) {
                            if (n.isUnsigned12Bits()) {
                                yield List.of(new JumpZeroStatement(parserContext.getHere(), new AddressArgument.Resolved(n.getValue())));
                            } else {
                                throw new OctoAssemblerException("The 'jump0' statement target '%d' does not fit in 12 bits!".formatted(n.getValue()), n.getSourcePosition());
                            }
                        } else {
                            yield List.of(new JumpZeroStatement(parserContext.getHere(), new AddressArgument.NamedLabelReference(token)));
                        }
                    }
                    case HIRES -> List.of(new HiresStatement(parserContext.getHere()));
                    case LORES -> List.of(new LoresStatement(parserContext.getHere()));
                    case SCROLL_DOWN -> {
                        IntegerLiteralToken n = parserContext.pollTokenOrThrow(IntegerLiteralToken.class, "Expected integer operand in 'scroll-down' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The operand '%s' is not an integer!".formatted(token.getLexeme()));
                        if (n.isUnsigned4Bits()) {
                            yield List.of(new ScrollDownStatement(parserContext.getHere(), n.getValue()));
                        } else {
                            throw new OctoAssemblerException("The 'scroll-down' statement operand '%d' for 'n' does not fit in 4 bits. Must be in the range [0, 15]!".formatted(n.getValue()), n.getSourcePosition());
                        }
                    }
                    case SCROLL_LEFT -> List.of(new ScrollLeftStatement(parserContext.getHere()));
                    case SCROLL_RIGHT -> List.of(new ScrollRightStatement(parserContext.getHere()));
                    case EXIT -> List.of(new ExitStatement(parserContext.getHere()));
                    case SAVE_FLAGS -> List.of(new SaveFlagsStatement(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' operand in 'saveflags' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                    case LOAD_FLAGS -> List.of(new LoadFlagsStatement(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' operand in 'loadflags' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The argument '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                    case PLANE -> {
                        IntegerLiteralToken n = parserContext.pollTokenOrThrow(IntegerLiteralToken.class, "Expected integer operand in 'plane' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The operand '%s' is not an integer!".formatted(token.getLexeme()));
                        if (n.isUnsigned4Bits()) {
                            yield List.of(new SetBitplanesStatement(parserContext.getHere(), n.getValue()));
                        } else {
                            throw new OctoAssemblerException("The 'plane' statement operand %d for 'n' does not fit in 4 bits. Must be in the range [0, 15]!".formatted(n.getValue()), n.getSourcePosition());
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
                        IntegerLiteralToken n = parserContext.pollTokenOrThrow(IntegerLiteralToken.class, "Expected integer operand in 'scroll-up' statement!", instructionStatementNameToken.getSourcePosition(), token -> "The operand '%s' is not an integer!".formatted(token.getLexeme()));
                        if (n.isUnsigned4Bits()) {
                            yield List.of(new ScrollUpStatement(parserContext.getHere(), n.getValue()));
                        } else {
                            throw new OctoAssemblerException("The 'scroll-up' statement operand '%d' for 'n' does not fit in 4 bits. Must be in the range [0, 15]!".formatted(n.getValue()), n.getSourcePosition());
                        }
                    }
            };
        };
    }

    private Collection<CodePrimitive> parseLiteral(ParserContext parserContext, LiteralToken literalToken) throws OctoAssemblerException {
        return switch (literalToken) {
            case RegisterLiteralToken registerLiteralToken ->
                    this.parseRegisterLiteral(parserContext, registerLiteralToken);
            case IntegerLiteralToken integerLiteralToken -> {
                if (integerLiteralToken.is8Bits()) {
                    yield List.of(new BytePrimitive(parserContext.getHere(), integerLiteralToken.getValue()));
                } else {
                    throw new OctoAssemblerException("Raw integer literal '%d' does not fit in 8 bits [-128, 255]".formatted(integerLiteralToken.getValue()), integerLiteralToken.getSourcePosition());
                }
            }
            case FloatLiteralToken floatLiteralToken -> parserContext.checkReservedName(floatLiteralToken);
            case StringLiteralToken stringLiteralToken -> parserContext.checkReservedName(stringLiteralToken);
        };
    }

    private Collection<CodePrimitive> parseAssignmentKeywordToken(ParserContext parserContext, AssignmentKeywordToken assignmentKeywordToken) throws OctoAssemblerException {
        return (switch (assignmentKeywordToken.getAssignmentKeyword()) {
            case DELAY -> {
                if (parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operator ':=' after 'delay' assignment!", assignmentKeywordToken.getSourcePosition(), token -> "Unknown assignment operator '%s'!".formatted(token.getLexeme())).getAssignmentOperation() == AssignmentOperation.SET) {
                    yield List.of(new SetDelayTimerAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' operand after delay statement!", assignmentKeywordToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                } else {
                    throw new OctoAssemblerException("Expected assignment operator ':=' after delay statement!", assignmentKeywordToken.getSourcePosition());
                }
            }
            case BUZZER -> {
                if (parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operator ':=' after 'buzzer' assignment!", assignmentKeywordToken.getSourcePosition(), token -> "Unknown assignment operator '%s'!".formatted(token.getLexeme())).getAssignmentOperation() == AssignmentOperation.SET) {
                    yield List.of(new SetSoundTimerAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' operand after delay statement!", assignmentKeywordToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                } else {
                    throw new OctoAssemblerException("Expected assignment operator ':=' after buzzer statement!", assignmentKeywordToken.getSourcePosition());
                }
            }
            case PITCH -> {
                if (parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operator ':=' after 'pitch' assignment!", assignmentKeywordToken.getSourcePosition(), token -> "Unknown assignment operator '%s'!".formatted(token.getLexeme())).getAssignmentOperation() == AssignmentOperation.SET) {
                    yield List.of(new SetPitchAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' operand after delay statement!", assignmentKeywordToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                } else {
                    throw new OctoAssemblerException("Expected assignment operator ':=' after pitch statement!", assignmentKeywordToken.getSourcePosition());
                }
            }
            default -> parserContext.checkReservedName(assignmentKeywordToken);
        });
    }

    private Collection<CodePrimitive> parseRegisterLiteral(ParserContext parserContext, RegisterLiteralToken vx) throws OctoAssemblerException {
        return switch (parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operator after register assignment!", vx.getSourcePosition(), token -> "Unknown register assignment operator '%s'!".formatted(token.getLexeme())).getAssignmentOperation()) {
            case BITWISE_OR -> List.of(new BitwiseORRegistersAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' operand after 'vx |=' assignment!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case BITWISE_AND -> List.of(new BitwiseANDRegistersAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' operand after 'vx &=' assignment!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case BITWISE_XOR -> List.of(new BitwiseXORRegistersAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' operand after 'vx ^=' assignment!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case RIGHT_SHIFT -> List.of(new RightShiftRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' operand after 'vx >>=' assignment!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case LEFT_SHIFT -> List.of(new LeftShiftRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' operand after 'vx <<=' assignment!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case RIGHT_SUBTRACT -> List.of(new RightSubtractRegisterFromRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vy' operand after 'vx =-' assignment!", vx.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case ADD -> switch (parserContext.pollTokenOrThrow("Expected operand after 'vx +=' statement!", vx.getSourcePosition())) {
                case RegisterLiteralToken vy -> List.of(new AddRegisterToRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex()));
                case IntegerLiteralToken nn -> {
                    if (nn.is8Bits()) {
                        yield List.of(new AddConstantToRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), nn.getValue()));
                    } else {
                        throw new OctoAssemblerException("Operand '%d' for 'vx +=' assignment does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(nn.getValue()), nn.getSourcePosition());
                    }
                }
                case Token token -> throw new OctoAssemblerException("Operand '%s' for 'vx +=' statement is not a register or number!".formatted(token.getLexeme()), token.getSourcePosition());
            };
            case LEFT_SUBTRACT -> switch (parserContext.pollTokenOrThrow("Expected operand after 'vx -=' statement!", vx.getSourcePosition())) {
                case RegisterLiteralToken vy -> List.of(new LeftSubtractRegisterFromRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex()));
                case IntegerLiteralToken nn -> {
                    if (nn.is8Bits()) {
                        yield List.of(new AddConstantToRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), -nn.getValue()));
                    } else {
                        throw new OctoAssemblerException("Operand '%d' for 'vx -=' assignment does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(nn.getValue()), nn.getSourcePosition());
                    }
                }
                case Token token -> throw new OctoAssemblerException("Operand '%s' for 'vx -=' statement is not a register or a number!".formatted(token.getLexeme()), token.getSourcePosition());
            };
            case SET -> switch (parserContext.pollTokenOrThrow("Expected register or number operand after 'vx :=' statement!", vx.getSourcePosition())) {
                case RegisterLiteralToken vy -> List.of(new SetRegisterToRegisterAssignment(parserContext.getHere(), vx.getRegisterIndex(), vy.getRegisterIndex()));
                case IntegerLiteralToken nn -> {
                    if (nn.is8Bits()) {
                        yield List.of(new SetRegisterToConstantAssignment(parserContext.getHere(), vx.getRegisterIndex(), nn.getValue()));
                    } else {
                        throw new OctoAssemblerException("Operand '%d' for 'vx :=' does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(nn.getValue()), nn.getSourcePosition());
                    }
                }
                case AssignmentKeywordToken assignmentKeywordToken ->
                    switch (assignmentKeywordToken.getAssignmentKeyword()) {
                        case DELAY -> List.of(new SetRegisterToDelayTimerAssignment(parserContext.getHere(), vx.getRegisterIndex()));
                        case KEY -> List.of(new SetRegisterToKeyAssignment(parserContext.getHere(), vx.getRegisterIndex()));
                        case RANDOM -> {
                            IntegerLiteralToken nn = parserContext.pollTokenOrThrow(IntegerLiteralToken.class, "Expected integer operand after 'vx := random' assignment!", vx.getSourcePosition(), token -> "The argument '%s' is not an integer!".formatted(token.getLexeme()));
                            if (nn.is8Bits()) {
                                yield List.of(new SetRegisterToRandomAssignment(parserContext.getHere(), vx.getRegisterIndex(), nn.getValue()));
                            } else {
                                throw new OctoAssemblerException("Operand '%d' for 'vx := random' does not fit in 8 bits. Must be in the range [-128, 255]!".formatted(nn.getValue()), nn.getSourcePosition());
                            }
                        }
                        default -> parserContext.checkReservedName(assignmentKeywordToken);
                    };
                case Token token -> throw new OctoAssemblerException("Unexpected operand '%s' for 'vx :=' statement".formatted(token.getLexeme()), token.getSourcePosition());
            };
        };
    }

    private Collection<CodePrimitive> parseIndexRegister(ParserContext parserContext, IndexRegisterToken indexRegisterToken) throws OctoAssemblerException {
        AssignmentOperatorToken assignmentOperatorToken = parserContext.pollTokenOrThrow(AssignmentOperatorToken.class, "Expected assignment operators ':=' or '+=' after an 'i' assignment!", indexRegisterToken.getSourcePosition(), token -> "Unknown assignment operator '%s' for 'i' assignment!".formatted(token.getLexeme()));
        return switch (assignmentOperatorToken.getAssignmentOperation()) {
            case ADD -> List.of(new AddRegisterToIndexRegisterAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' operand after 'i' increment statement!", assignmentOperatorToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
            case SET -> switch (parserContext.pollTokenOrThrow("Expected operand after 'i' assignment!", assignmentOperatorToken.getSourcePosition())) {
                case IntegerLiteralToken nnn -> {
                    if (nnn.isUnsigned12Bits()) {
                        yield List.of(new SetIndexRegisterToConstantAssignment(parserContext.getHere(), new AddressArgument.Resolved(nnn.getValue())));
                    } else {
                        throw new OctoAssemblerException("Operand '%d' for 'i' assignment does not fit in 12 bits!".formatted(nnn.getValue()), nnn.getSourcePosition());
                    }
                }
                case AssignmentKeywordToken assignmentKeywordToken -> switch (assignmentKeywordToken.getAssignmentKeyword()) {
                    case HEX -> List.of(new SetIndexRegisterToHexCharAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' operand after 'i := hex' assignment!", assignmentKeywordToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                    case BIGHEX -> List.of(new SetIndexRegisterToBigHexCharAssignment(parserContext.getHere(), parserContext.pollTokenOrThrow(RegisterLiteralToken.class, "Expected 'vx' operand after 'i := bighex' increment statement!", assignmentKeywordToken.getSourcePosition(), token -> "The operand '%s' is not a register!".formatted(token.getLexeme())).getRegisterIndex()));
                    case LONG -> {
                        Token longAssignmentArgumentToken = parserContext.pollTokenOrThrow("Expected operand after 'i := long' assignment!", assignmentOperatorToken.getSourcePosition());
                        if (longAssignmentArgumentToken instanceof IntegerLiteralToken nnnn) {
                            if (nnnn.isUnsigned16Bits()) {
                                yield List.of(new SetIndexRegisterToLongConstantAssignment(parserContext.getHere(), new AddressArgument.Resolved(nnnn.getValue())));
                            } else {
                                throw new OctoAssemblerException("Operand '%d' for 'i := long' assignment does not fit in 16 bits!".formatted(nnnn.getValue()), nnnn.getSourcePosition());
                            }
                        } else {
                            parserContext.checkReservedName(longAssignmentArgumentToken);
                            yield List.of(new SetIndexRegisterToLongConstantAssignment(parserContext.getHere(), new AddressArgument.NamedLabelReference(longAssignmentArgumentToken)));
                        }
                    }
                    default -> throw new OctoAssemblerException("Unexpected operand '%s' after 'i' assignment statement!".formatted(assignmentKeywordToken.getLexeme()), assignmentKeywordToken.getSourcePosition());
                };
                case Token labelToken -> {
                    parserContext.checkReservedName(labelToken);
                    yield List.of(new SetIndexRegisterToConstantAssignment(parserContext.getHere(), new AddressArgument.NamedLabelReference(labelToken)));
                }
            };
            default -> throw new OctoAssemblerException("Unknown assignment operator '%s' after 'i' assignment!".formatted(assignmentOperatorToken.getLexeme()), assignmentOperatorToken.getSourcePosition());
        };
    }

    private Collection<CodePrimitive> parseDirective(ParserContext parserContext, DirectiveToken directiveToken) throws OctoAssemblerException {
        return switch (directiveToken.getDirective()) {
            case LABEL_DEFINITION -> {
                Token token = parserContext.pollTokenOrThrow("Expected label name following ':' directive!", directiveToken.getSourcePosition());
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

    private Collection<CodePrimitive> invertSkipConditions(Collection<CodePrimitive> codePrimitives) {
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

    private class ParserContext {

        private final SourceStream<Token> tokenStream;
        private final List<CodeElement> codeElements = new ArrayList<>();
        private final Map<String, DirectiveDefinition> directiveDefinitions = new HashMap<>();
        private final Map<InternalLabelKey, Integer> internalLabelDefinitions = new HashMap<>();
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

        private Map<InternalLabelKey, Integer> getInternalLabelDefinitions() {
            return Map.copyOf(this.internalLabelDefinitions);
        }

        private boolean foundMainLabel() {
            return this.foundMainLabel;
        }

        private void addCodeElement(CodeElement codeElement) throws OctoAssemblerException {
            if (codeElement instanceof LabelableElement labelableElement && labelableElement.getAddressArgument() instanceof AddressArgument.NamedLabelReference(
                    Token token
            )) {
                this.checkReservedName(token);
            }
            this.codeElements.add(codeElement);
        }

        private void addDirectiveDefinition(Token token, DirectiveDefinition directiveDefinition) throws OctoAssemblerException {
            String name = token.getLexeme();
            if (this.directiveDefinitions.containsKey(name)) {
                throw new OctoAssemblerException("Directive name '%s' is already defined!".formatted(name), token.getSourcePosition());
            }
            this.checkReservedName(token);
            if (directiveDefinition instanceof LabelDefinition labelDefinition && "main".equals(labelDefinition.getName())) {
                this.foundMainLabel = true;
                if (labelDefinition.getAddress() == programStart + 2 || labelDefinition.getAddress() == programStart) {
                    this.here = 0x200;
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

        private void incrementHere(@Nullable Token token, CodePrimitive codePrimitive) throws OctoAssemblerException {
            this.incrementHere(token, codePrimitive.getSizeInBytes());
        }

        private void incrementHere(CodePrimitive codePrimitive) throws OctoAssemblerException {
            this.incrementHere(null, codePrimitive);
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
            return this.tokenStream.poll()
                    .flatMap(token -> this.getDirective(token)
                            .map(directiveDefinition -> {
                                if (directiveDefinition instanceof ExpandableDirective expandableDirective) {
                                    this.expandDirective(expandableDirective, token.getSourcePosition());
                                    return this.pollToken();
                                } else {
                                    return Optional.of(token);
                                }
                            }).orElse(Optional.of(token)));
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
            return this.tokenStream.peek()
                    .flatMap(token -> this.getDirective(token)
                            .map(directiveDefinition -> {
                                if (directiveDefinition instanceof ExpandableDirective expandableDirective) {
                                    this.expandDirective(expandableDirective, token.getSourcePosition());
                                    return this.peekToken();
                                } else {
                                    return Optional.of(token);
                                }
                            }).orElse(Optional.of(token)));
        }

        private void expandDirective(ExpandableDirective expandableDirective, SourcePosition sourcePosition) {
            List<Token> directiveTokens = expandableDirective.expand(this.here, sourcePosition);
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

        private Collection<CodePrimitive> checkReservedName(Token token) throws OctoAssemblerException {
            if (token instanceof ReservedNameToken) {
                throw new OctoAssemblerException("The name '%s' is reserved and cannot be used as a label!".formatted(token.getLexeme()), token.getSourcePosition());
            } else {
                return List.of(new CallStatement(this.here, new AddressArgument.NamedLabelReference(token)));
            }
        }

    }

}
