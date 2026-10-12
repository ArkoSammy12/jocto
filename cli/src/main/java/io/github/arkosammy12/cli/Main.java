package io.github.arkosammy12.cli;

import picocli.CommandLine;

public class Main {

    static void main(String[] args) {
        int exitCode = new CommandLine(new JoctoCLI()).execute(args);
        System.exit(exitCode);
    }

}
