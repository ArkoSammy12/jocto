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
            CodegenContext codegenContext = new CodegenContext(labelDefinitions, internalLabelDefinitions);
            for (CodeElement codeElement : codeElements) {
                switch (codeElement) {
                    case CodeBlock codeBlock -> {
                        for (CodePrimitive codePrimitive : codeBlock.expand()) {
                            this.consumePrimitive(codegenContext, codePrimitive);
                        }
                    }
                    case CodePrimitive codePrimitive -> this.consumePrimitive(codegenContext, codePrimitive);
                }
            }
            return new OctoCodegenResult.Ok(codegenContext.getBytes());
        } catch (OctoAssemblerException e) {
            return e.toCodegenResult();
        }
    }

    private void consumePrimitive(CodegenContext codegenContext, CodePrimitive codePrimitive) throws OctoAssemblerException {
        if (codePrimitive instanceof LabelableElement labelableElement && labelableElement.getAddressArgument() instanceof AddressArgument.Unresolved unresolvedAddress) {
            switch (unresolvedAddress) {
                case AddressArgument.NamedLabelReference(Token token) -> {
                    Optional<LabelDefinition> optionalLabelDefinition = codegenContext.getLabelDefinition(token.getLexeme());
                    if (optionalLabelDefinition.isPresent()) {
                        LabelDefinition labelDefinition = optionalLabelDefinition.get();
                        switch (labelableElement.resolve(labelDefinition.getAddress())) {
                            case LabelResolveResult.Ok _ -> {}
                            case LabelResolveResult.AlreadyResolved _ -> throw new OctoAssemblerException("The label '%s' has already been defined!".formatted(labelDefinition.getAddress()), unresolvedAddress.getSourcePosition());
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
                            case LabelResolveResult.AlreadyResolved _ -> throw new OctoAssemblerException("The internal label '%s' has already been defined!".formatted(internalLabelKey), unresolvedAddress.getSourcePosition());
                            case LabelResolveResult.LabelResolveError(String error, SourcePosition sourcePosition) -> throw new OctoAssemblerException(error, sourcePosition);
                        }
                    }
                }
            }
        }
        switch (codePrimitive.getBytes()) {
            case BytesResult.Data(byte[] bytes) -> codegenContext.addBytes(bytes, codePrimitive.getOffset());
            case BytesResult.UnresolvedLabel(AddressArgument.Unresolved unresolved) -> throw switch (unresolved) {
                case AddressArgument.NamedLabelReference(Token token) -> new OctoAssemblerException("Unresolved label '%s'".formatted(token.getLexeme()), token.getSourcePosition());
                case AddressArgument.InternalLabelReference(InternalLabelKey internalLabelKey) -> new OctoAssemblerException("Unresolved internal label '%s'".formatted(internalLabelKey), internalLabelKey.sourcePositionKey());
            };
        }
    }

    private class CodegenContext {

        private final Map<String, LabelDefinition> labelDefinitions;
        private final Map<InternalLabelKey, Integer> internalLabelDefinitions;
        private final ArrayList<Byte> bytes = new ArrayList<>();

        private CodegenContext(Map<String, LabelDefinition> labelDefinitions, Map<InternalLabelKey, Integer> internalLabelDefinitions) {
            this.labelDefinitions = labelDefinitions;
            this.internalLabelDefinitions = internalLabelDefinitions;
        }

        private Optional<LabelDefinition> getLabelDefinition(String name) {
            return Optional.ofNullable(this.labelDefinitions.get(name));
        }

        private Optional<Integer> getInternalLabelDefinition(InternalLabelKey internalLabelKey) {
            return Optional.ofNullable(this.internalLabelDefinitions.get(internalLabelKey));
        }

        private byte[] getBytes() throws OctoAssemblerException {
            byte[] rawBytes = new byte[this.bytes.size()];
            for (int i = 0; i < this.bytes.size(); i++) {
                Byte byteElement = this.bytes.get(i);
                rawBytes[i] = byteElement == null ? (byte) 0x00 : byteElement;
            }
            return rawBytes;
        }

        private void addBytes(byte[] bytes, int offset) throws OctoAssemblerException {
            int absoluteOffset = offset - programStart;
            if (absoluteOffset < 0) {
                throw new IllegalArgumentException("Tried to write bytes to ROM before program start!");
            }

            int requiredSize = absoluteOffset + bytes.length;
            if (requiredSize > this.bytes.size()) {
                this.bytes.ensureCapacity(requiredSize);
                while (requiredSize > this.bytes.size()) {
                    this.bytes.add(null);
                }
            }

            for (int i = 0; i < bytes.length; i++) {
                int romOffset = i + absoluteOffset;
                if (this.bytes.get(romOffset) == null) {
                    this.bytes.set(romOffset, bytes[i]);
                } else {
                    throw new OctoAssemblerException("Attempted to write byte '%02X' at already occupied offset of '%04X'".formatted(bytes[i], offset));
                }
            }
        }

    }

}
