package io.github.arkosammy12.cli;

import io.github.arkosammy12.core.assembler.OctoAssembler;
import io.github.arkosammy12.core.lexer.SourceFile;
import io.github.arkosammy12.core.result.OctoAssemblerResult;
import io.github.arkosammy12.core.result.OctoCodegenResult;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "jocto",
        mixinStandardHelpOptions = true,
        versionProvider = MavenProperties.Provider.class,
        description = "Assembles the provided .8o source file"
)
public final class JoctoCLI implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-i", "--input"},
            required = true,
            description = "The path of the .8o source file to assemble"
    )
    private Path inputPath;

    @CommandLine.Option(
            names = {"-o", "--output"},
            defaultValue = CommandLine.Option.NULL_VALUE,
            description = "The path of the assembled binary file, or input.out.ch8"
    )
    @Nullable
    private Path outputPath;

    @Override
    public Integer call() throws Exception {
        SourceFile sourceFile = new SourceFile(this.inputPath);
        OctoAssembler octoAssembler = new OctoAssembler(0x200);
        OctoAssemblerResult octoAssemblerResult = octoAssembler.assemble(sourceFile);
        return switch (octoAssemblerResult) {
            case OctoAssemblerResult.Error error -> {
                IO.println("%s%s".formatted(error.getSourcePosition().map(sourcePosition -> "(%d:%d) ".formatted(sourcePosition.row() + 1, sourcePosition.column() + 1)).orElse(""), error.getError()));
                yield 1;
            }
            case OctoCodegenResult.Ok(byte[] rom) -> {
                Path outputPath;
                if (this.outputPath != null) {
                    outputPath = this.outputPath;
                } else {
                    Path inputFilePath = this.inputPath.getFileName();
                    if (inputFilePath == null) {
                        throw new IllegalArgumentException("The input path '%s' does not correspond to a valid file!".formatted(this.inputPath.toString()));
                    }
                    outputPath = this.inputPath.resolveSibling(inputFilePath + ".out.ch8");
                }

                Path parentDirectory = outputPath.getParent();
                if (parentDirectory != null) {
                    Files.createDirectories(parentDirectory);
                }

                if (Files.exists(outputPath)) {
                    IO.println("The output path '%s' already exists!".formatted(outputPath));
                    yield 1;
                }

                Files.write(outputPath, rom);
                IO.println("Assembled binary has been written to '%s'".formatted(outputPath));
                yield 0;
            }
            default -> {
                IO.println("Failed to assemble input source file!");
                yield 1;
            }
        };
    }

}
