package io.github.arkosammy12.core.test.assembler;

import io.github.arkosammy12.core.assembler.OctoAssembler;
import io.github.arkosammy12.core.lexer.SourceFile;
import io.github.arkosammy12.core.result.OctoAssemblerResult;
import io.github.arkosammy12.core.result.OctoCodegenResult;
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
    public Stream<DynamicTest> runCOctoTests() throws URISyntaxException, IOException {
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
                            String expectedByteString = firstDifference < expected.length ? formatByte(expected[firstDifference]) : "N/A";
                            String actualByteString = firstDifference < produced.length ? formatByte(produced[firstDifference]) : "N/A";
                            this.failCOctoTest(sourceFilePath, "Mismatched byte found at offset: %s".formatted(formatOffset(firstDifference)), expectedByteString, actualByteString, expectedFailures);
                        }
                    }
                    case OctoAssemblerResult result -> this.failCOctoTest(sourceFilePath, "Failed to assemble source file", "Successful assembly with no errors", result.getString(), expectedFailures);
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
                        String errorString = Files.readAllLines(errorFilePath).stream().map(line -> line.replace("\n", "")).filter(line -> !line.isBlank()).collect(Collectors.joining(". "));
                        this.failCOctoTest(sourceFilePath, "Expected assembly error but assembly succeeded!", errorString, ok.getString(), expectedFailures);
                    }
                }
                return;
            }
            abort("Found no matching binary or error output file for Octo source file '%s'. Aborting!".formatted(sourceFilePath));
        }));
    }

    private static String formatByte(int value) {
        return "%d ($%02X)".formatted(value, value);
    }

    private static String formatOffset(int value) {
        return "%d ($%04X)".formatted(value, value);
    }

    private void failCOctoTest(Path sourceFilePath, String reason, String expected, String actual, Collection<String> expectedFailures) {
        String sourceFileName = sourceFilePath.getFileName().toString();
        boolean isExpected = expectedFailures.contains(sourceFileName);
        String failureText = """
                %s: %s
                Reason: %s
                Expected: %s
                Actual: %s
                """.formatted(
                        isExpected ? "Skipping failed c-octo test for source file" : "Failed c-octo test for source file",
                        sourceFileName,
                        reason,
                        expected,
                        actual
                );
        if (isExpected) {
            abort(failureText);
        } else {
            fail(failureText);
        }
    }

    public static int compareBytes(byte[] first, byte[] second) {
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
