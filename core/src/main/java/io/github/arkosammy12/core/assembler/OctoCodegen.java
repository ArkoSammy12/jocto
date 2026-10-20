package io.github.arkosammy12.core.assembler;

import io.github.arkosammy12.core.elements.*;
import io.github.arkosammy12.core.result.OctoCodegenResult;
import io.github.arkosammy12.core.result.OctoAssemblerException;
import io.github.arkosammy12.core.lexer.SourcePosition;
import io.github.arkosammy12.core.parser.AddressArgument;
import io.github.arkosammy12.core.parser.InternalLabelKey;
import io.github.arkosammy12.core.parser.directive.LabelDefinition;
import io.github.arkosammy12.core.token.Token;

import java.util.*;

public class OctoCodegen {

    private final int programStart;

    public OctoCodegen(int programStart) {
        if (programStart < 0) {
            throw new IllegalArgumentException("The program start value cannot be less than zero!");
        }
        this.programStart = programStart;
    }

    public OctoCodegenResult generateCode(Collection<CodeElement> codeElements, Map<String, LabelDefinition> labelDefinitions, Map<InternalLabelKey, Integer> internalLabelDefinitions) {
        try {
            OctoCodegenContext codegenContext = new OctoCodegenContext(this.programStart, labelDefinitions, internalLabelDefinitions);
            for (CodeElement codeElement : codeElements) {
                this.consumePrimitive(codegenContext, codeElement);
            }
            return new OctoCodegenResult.Ok(codegenContext.getBytes());
        } catch (OctoAssemblerException e) {
            return e.toCodegenResult();
        }
    }

    private void consumePrimitive(OctoCodegenContext codegenContext, CodeElement codeElement) throws OctoAssemblerException {
        if (codeElement instanceof LabelableElement labelableElement && labelableElement.getAddressArgument() instanceof AddressArgument.Unresolved unresolvedAddress) {
            switch (unresolvedAddress) {
                case AddressArgument.NamedLabelReference(Token token) -> {
                    Optional<LabelDefinition> optionalLabelDefinition = codegenContext.getLabelDefinition(token.getLexeme());
                    if (optionalLabelDefinition.isPresent()) {
                        LabelDefinition labelDefinition = optionalLabelDefinition.get();
                        switch (labelableElement.resolve(labelDefinition.getAddress())) {
                            case LabelResolveResult.Ok _ -> {}
                            case LabelResolveResult.AlreadyResolved _ -> /*throw new OctoAssemblerException("The label '%s' has already been defined!".formatted(labelDefinition.getAddress()), unresolvedAddress.getSourcePosition());*/ {}
                            case LabelResolveResult.LabelResolveError(String error, SourcePosition sourcePosition) -> throw new OctoAssemblerException(error, sourcePosition);
                        }
                    }
                }
                case AddressArgument.InternalLabelReference(InternalLabelKey internalLabelKey) -> {
                    Optional<Integer> optionalInternalAddress = codegenContext.getInternalLabelDefinition(internalLabelKey);
                    if (optionalInternalAddress.isPresent()) {
                        int address = optionalInternalAddress.get();
                        switch (labelableElement.resolve(address)) {
                            case LabelResolveResult.Ok _ -> {}
                            case LabelResolveResult.AlreadyResolved _ -> /*throw new OctoAssemblerException("The internal label '%s' has already been defined!".formatted(internalLabelKey), unresolvedAddress.getSourcePosition());*/ {}
                            case LabelResolveResult.LabelResolveError(String error, SourcePosition sourcePosition) -> throw new OctoAssemblerException(error, sourcePosition);
                        }
                    }
                }
            }
        }
        switch (codeElement.getBytes()) {
            case BytesResult.Data(byte[] bytes) -> codegenContext.addBytes(bytes, codeElement.getOffset());
            case BytesResult.UnresolvedLabel(AddressArgument.Unresolved unresolved, _) -> throw switch (unresolved) {
                case AddressArgument.NamedLabelReference(Token token) -> new OctoAssemblerException("Unresolved label '%s'".formatted(token.getLexeme()), token.getSourcePosition());
                case AddressArgument.InternalLabelReference(InternalLabelKey internalLabelKey) -> new OctoAssemblerException("Unresolved internal label '%s'".formatted(internalLabelKey), internalLabelKey.sourcePositionKey());
            };
        }
    }

}
