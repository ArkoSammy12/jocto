package io.github.arkosammy12.core.test.assembler;

import io.github.arkosammy12.core.assembler.OctoAssembler;
import io.github.arkosammy12.core.lexer.SourceFile;
import io.github.arkosammy12.core.result.OctoAssemblerResult;
import io.github.arkosammy12.core.result.OctoCodegenResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.*;

public class OctoAssemblerTests {

    private static final String C_OCTO_TEST_DIR = "c-octo-tests";
    private static final String EXPECTED_FAILURES_FILE_NAME = "expected-failures.txt";
    private static final int PROGRAM_START = 0x200;

    @TestFactory
    @SuppressWarnings("resource")
    public Stream<DynamicTest> runExpectedSuccessTests() throws URISyntaxException, IOException {
        URL cOctoTestsUrl = this.getClass().getClassLoader().getResource(C_OCTO_TEST_DIR);
        assumeFalse(cOctoTestsUrl == null, () -> "The '%s' test directory URL could not be found. Aborting tests!".formatted(C_OCTO_TEST_DIR));

        Path cOctoTestDirectoryPath = Paths.get(cOctoTestsUrl.toURI());
        Path expectedFailuresFilePath = cOctoTestDirectoryPath.resolve(EXPECTED_FAILURES_FILE_NAME);
        assumeTrue(Files.exists(expectedFailuresFilePath), () -> "Could not find expected failures file at '%s'. Aborting!".formatted(expectedFailuresFilePath));

        Collection<String> expectedFailures = Files.readAllLines(expectedFailuresFilePath);
        return Files.list(cOctoTestDirectoryPath).filter(path -> path.getFileName().toString().endsWith(".8o")).map(sourceFilePath -> DynamicTest.dynamicTest("Testing Octo source file: '%s'".formatted(sourceFilePath.getFileName().toString()), () -> {
            Path binaryFilePath = sourceFilePath.resolveSibling(sourceFilePath.getFileName().toString().replace(".8o", ".ch8"));
            if (Files.exists(binaryFilePath)) {
                OctoAssembler octoAssembler = new OctoAssembler(PROGRAM_START);
                OctoAssemblerResult octoAssemblerResult = octoAssembler.assemble(new SourceFile(sourceFilePath));
                switch (octoAssemblerResult) {
                    case OctoCodegenResult.Ok codegenResult -> {
                        byte[] produced = codegenResult.rom();
                        byte[] expected = Files.readAllBytes(binaryFilePath);
                        int firstDifference = compareBytes(produced, expected);
                        if (firstDifference >= 0) {
                            if (expectedFailures.contains(sourceFilePath.getFileName().toString())) {
                                abort(() -> """
                                    Skipping failure for source file test: %s
                                    """.formatted(sourceFilePath.getFileName()));
                            } else {
                                String expectedByteString = firstDifference < expected.length ? "%d ($%02X)".formatted(expected[firstDifference], expected[firstDifference]) : "N/A";
                                String actualByteString = firstDifference < produced.length ? "%d ($%02X)".formatted(produced[firstDifference], produced[firstDifference]) : "N/A";
                                fail(() -> """
                                    Binary mismatch for source file: %s
                                    Mismatched byte found at offset: %d ($%04X)
                                    Expected byte: %s
                                    Actual byte: %s
                                    """.formatted(sourceFilePath.getFileName(), firstDifference, firstDifference, expectedByteString, actualByteString));
                            }
                        }
                    }
                    case OctoAssemblerResult result -> {
                        if (expectedFailures.contains(sourceFilePath.getFileName().toString())) {
                            abort(() -> """
                                    Skipping failure for source file test: %s
                                    """.formatted(sourceFilePath.getFileName()));
                        } else {
                            fail(() -> """
                                    Failed to assemble source file: %s
                                    Expected: Successful assembly with no errors
                                    Actual: %s
                                    """.formatted(sourceFilePath.getFileName(), result.getString()));
                        }
                    }
                }
                return;
            }
            Path errorFilePath = sourceFilePath.resolveSibling(sourceFilePath.getFileName().toString().replace(".8o", ".err"));
            if (Files.exists(errorFilePath)) {
                OctoAssembler octoAssembler = new OctoAssembler(PROGRAM_START);
                OctoAssemblerResult octoAssemblerResult = octoAssembler.assemble(new SourceFile(sourceFilePath));
                switch (octoAssemblerResult) {
                    case OctoAssemblerResult.Error _ -> {}
                    case OctoAssemblerResult.Ok ok -> {
                        if (expectedFailures.contains(sourceFilePath.getFileName().toString())) {
                            abort(() -> """
                                    Skipping failure for source file test: %s
                                    """.formatted(sourceFilePath.getFileName()));
                        } else {
                            String errorString = Files.readAllLines(errorFilePath).stream().map(line -> line.replace("\n", "")).filter(line -> !line.isBlank()).collect(Collectors.joining(". "));
                            fail(() -> """
                                    Expected assembly error but assembly succeeded for source file: %s
                                    Expected: %s
                                    Actual: %s
                                    """.formatted(sourceFilePath.getFileName(), errorString, ok.getString()));
                        }
                    }
                }
                return;
            }
            Assumptions.abort("Found no matching binary or error output file for Octo source file '%s'. Aborting!".formatted(sourceFilePath));
        }));
    }

    public int compareBytes(byte[] first, byte[] second) {
        int minLength = Math.min(first.length, second.length);
        for (int i = 0; i < minLength; i++) {
            if (first[i] != second[i]) {
                return i;
            }
        }
        if (first.length != second.length) {
            return minLength;
        }
        return -1;
    }

}
