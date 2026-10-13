# jocto
An implementation of the CHIP-8 Octo assembler written in Java
    
* [jocto](#jocto)
  * [Command-line Usage](#command-line-usage)
  * [Building](#building)
  * [References](#references)

# Command-line Usage

You can run the assembler with the `java -jar jocto-x.y.z.jar <args>` command. It is recommended to alias the JVM `-jar` invocation.

The CLI options are as follows:

```
Usage: jocto [-hV] -i=<inputPath> [-o=<outputPath>]
Assembles the provided .8o source file
  -h, --help                Show this help message and exit.
  -i, --input=<inputPath>   The path of the .8o source file to assemble
  -o, --output=<outputPath> The path of the assembled binary file, or input.out.
                              ch8
  -V, --version             Print version information and exit.
```

# Building

*jocto* is written in Java 25, so a Java Development Kit (JDK) targeting Java 25 or later to build this project.

Maven is used as the build and dependency management solution, though a wrapper is provided to avoid a local installation.
To build the project, clone or download the repository and run the following from the top-level directory:

## Windows

```
.\mvnw clean package
```

## macOS/Linux

```
./mvnw clean package
```

An output `.jar` should have then been generated in `/target/jocto-x.y.z.jar`.
To skip the automated tests, append the `-DskipTests` to the build command.

# References
- [John Earnest - Octo](https://github.com/JohnEarnest/Octo)
- [John Earnest - c-octo](https://github.com/JohnEarnest/c-octo)
- [Timendus - chipcode](https://github.com/Timendus/chipcode)
- [Timendus - Octopus2](https://github.com/Timendus/chipcode/tree/main/octopus2)
- [Gulrak - chiplet](https://github.com/gulrak/chiplet)