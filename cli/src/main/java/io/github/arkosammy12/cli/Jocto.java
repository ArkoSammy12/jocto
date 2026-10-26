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
public class Jocto implements Callable<Integer> {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(
            names = {"-i", "--input"},
            required = true,
            description = "The path of the Octo source file to assemble"
    )
    private Path inputPath;

    @CommandLine.Option(
            names = {"-o", "--output"},
            defaultValue = CommandLine.Option.NULL_VALUE,
            description = "The path of the assembled CHIP-8 binary program file, or input.ch8"
    )
    @Nullable
    private Path outputPath;

    @CommandLine.Option(
            names = {"-b", "--base-address"},
            defaultValue = "512",
            description = "The base address of the assembled .ch8 binary program"
    )
    private int baseAddress;

    @CommandLine.Option(
            names = {"-n", "--no-overwrite"},
            description = "Errors if the output file path already exists as a file"
    )
    private boolean noOverwrite;

    @Override
    public Integer call() throws Exception {
        return this.executeCommand(() -> {
            if (!Files.exists(this.inputPath)) {
                throw new CommandLine.ParameterException(spec.commandLine(), "The input file path '%s' does not exist!".formatted(this.inputPath));
            }

            Path outputPath;
            if (this.outputPath != null) {
                if (!Files.exists(this.outputPath)) {
                    throw new CommandLine.ParameterException(spec.commandLine(), "The output file path '%s' does not exist!".formatted(this.outputPath));
                }
                outputPath = this.outputPath;
            } else {
                outputPath = this.inputPath.resolveSibling(replaceFileExtensionWithCh8(this.inputPath.getFileName().toString()));
            }

            if (this.noOverwrite && Files.exists(outputPath)) {
                throw new CommandLine.ParameterException(spec.commandLine(), "The output file path '%s' already exists!".formatted(outputPath));
            }

            if (this.baseAddress < 0) {
                throw new CommandLine.ParameterException(spec.commandLine(), "The base address argument '%d' cannot be negative!".formatted(this.baseAddress));
            }

            IO.println("Assembling source file '%s'...".formatted(this.inputPath));
            SourceFile sourceFile = new SourceFile(this.inputPath);
            OctoAssembler assembler = new OctoAssembler(this.baseAddress);
            return switch (assembler.assemble(sourceFile)) {
                case OctoAssemblerResult.Error error -> {
                    IO.println("%s%s".formatted(error.getSourcePosition().map(sourcePosition -> "(%d:%d) ".formatted(sourcePosition.row() + 1, sourcePosition.column() + 1)).orElse(""), error.getError()));
                    yield -1;
                }
                case OctoCodegenResult.Ok(byte[] rom) -> {
                    Path outputPathParentDirectory = outputPath.getParent();
                    if (outputPathParentDirectory != null) {
                        Files.createDirectories(outputPathParentDirectory);
                    }
                    IO.println("Generated %d ($%02X) bytes".formatted(rom.length, rom.length));
                    Files.write(outputPath, rom);
                    IO.println("The assembled binary has been written to '%s'".formatted(outputPath));
                    yield 0;
                }
                case OctoAssemblerResult result -> {
                    IO.println("Failed to assemble input source file!\nAssembler result: %s".formatted(result));
                    yield -2;
                }
            };
        });
    }

    private Integer executeCommand(Callable<Integer> command) throws Exception {
        IO.println("%s v%s, by %s\n".formatted(MavenProperties.ARTIFACT_ID, MavenProperties.VERSION, MavenProperties.AUTHOR));
        boolean noExceptionThrown = false;
        long startTime = System.nanoTime();
        try {
            Integer result = command.call();
            noExceptionThrown = true;
            return result;
        } finally {
            long elapsedNanos = System.nanoTime() - startTime;
            if (noExceptionThrown) {
                double elapsedMillis = elapsedNanos / 1_000_000.0;
                IO.println("\nTook: %.2fms".formatted(elapsedMillis));
            }
        }
    }

    static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new Jocto());
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        System.exit(commandLine.execute(args));
    }

    private static String replaceFileExtensionWithCh8(String fileName) {
        int indexOfPeriod = fileName.lastIndexOf('.');
        if (indexOfPeriod >= 0) {
            return fileName.substring(0, indexOfPeriod) + ".ch8";
        } else {
            return fileName + ".ch8";
        }
    }

}
