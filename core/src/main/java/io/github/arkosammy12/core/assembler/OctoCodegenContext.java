package io.github.arkosammy12.core.assembler;

import io.github.arkosammy12.core.parser.InternalLabelKey;
import io.github.arkosammy12.core.parser.directive.LabelDefinition;
import io.github.arkosammy12.core.result.OctoAssemblerException;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

class OctoCodegenContext {

    private final int programStart;
    private final Map<String, LabelDefinition> labelDefinitions;
    private final Map<InternalLabelKey, Integer> internalLabelDefinitions;
    private final ArrayList<Byte> bytes = new ArrayList<>();

    OctoCodegenContext(int programStart, Map<String, LabelDefinition> labelDefinitions, Map<InternalLabelKey, Integer> internalLabelDefinitions) {
        if (programStart < 0) {
            throw new IllegalArgumentException("The program start value cannot be less than zero!");
        }
        this.programStart = programStart;
        this.labelDefinitions = labelDefinitions;
        this.internalLabelDefinitions = internalLabelDefinitions;
    }

    Optional<LabelDefinition> getLabelDefinition(String name) {
        return Optional.ofNullable(this.labelDefinitions.get(name));
    }

    Optional<Integer> getInternalLabelDefinition(InternalLabelKey internalLabelKey) {
        return Optional.ofNullable(this.internalLabelDefinitions.get(internalLabelKey));
    }

    byte[] getBytes() throws OctoAssemblerException {
        byte[] rawBytes = new byte[this.bytes.size()];
        for (int i = 0; i < this.bytes.size(); i++) {
            Byte byteElement = this.bytes.get(i);
            rawBytes[i] = byteElement == null ? (byte) 0x00 : byteElement;
        }
        return rawBytes;
    }

    void addBytes(byte[] bytes, int offset) throws OctoAssemblerException {
        int absoluteOffset = offset - this.programStart;
        if (absoluteOffset < 0) {
            return;
            //throw new IllegalArgumentException("Tried to write bytes to ROM before program start!");
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
