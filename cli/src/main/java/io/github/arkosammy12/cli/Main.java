package io.github.arkosammy12.cli;

import io.github.arkosammy12.core.assembler.OctoAssembler;
import io.github.arkosammy12.core.assembler.OctoAssemblyStage;
import io.github.arkosammy12.core.elements.CodeElement;
import io.github.arkosammy12.core.result.OctoAssemblerResult;
import io.github.arkosammy12.core.result.OctoCodegenResult;
import io.github.arkosammy12.core.lexer.*;
import io.github.arkosammy12.core.result.OctoParserResult;

import java.util.Collection;
import java.util.List;

import static io.github.arkosammy12.core.assembler.OctoAssembler.byteArrayToString;

public class Main {

    static void main(String[] args) {
        String test = """
                : main
                    if v1 == 0x00 begin
                        v2 := v3
                    else 
                        v4 := v5
                    end
                """;

        SourceFile sourceFile = new SourceFile(List.of(test.split("\n")));
        OctoAssembler assembler = new OctoAssembler(0x200, OctoAssemblyStage.CODEGEN);
        switch (assembler.assemble(sourceFile)) {
            case OctoParserResult.Ok(Collection<CodeElement> codeElements, _, _) -> codeElements.forEach(IO::println);
            case OctoCodegenResult.Ok(byte[] rom) -> IO.println(byteArrayToString(rom));
            case OctoAssemblerResult.Error error -> IO.println("""
                Assembly error!
                %s %s
                """.formatted(error.getSourcePosition().map(position -> "(%d:%d)".formatted(position.row() + 1, position.column() + 1)).orElse(""), error.getError()));
            default -> {}
        }
    }

}
