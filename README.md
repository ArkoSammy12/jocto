# jocto
An implementation of the CHIP-8 Octo assembler written in Java
    
* [jocto](#jocto)
  * [Command-line Usage](#command-line-usage)
  * [Building](#building)
  * [Notes](#notes)
  * [References](#references)

# Command-line Usage

Make sure to have a Java Development Kit (JDK) targeting Java 25 or later to run the built `.jar` file.

You can run the assembler with the `java -jar jocto-x.y.z.jar <args>` command. It is recommended to alias the JVM `-jar` invocation.

The CLI options are as follows:

```
Usage: jocto [-hnV] [-b=<baseAddress>] -i=<inputPath> [-o=<outputPath>]
Assembles the provided .8o source file
  -b, --base-address=<baseAddress>
                            The base address of the assembled .ch8 binary
                              program
  -h, --help                Show this help message and exit.
  -i, --input=<inputPath>   The path of the Octo source file to assemble
  -n, --no-overwrite        Errors if the output file path already exists as a
                              file
  -o, --output=<outputPath> The path of the assembled CHIP-8 binary program
                              file, or input.ch8
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

# Notes

The implementation of this assembler is largely based on the `c-octo` implementation of the Octo language by John Earnest.
Most semantics for directive arguments should follow that implementation.

**All debug directives (`:breakpoint`, `:monitor`, `:assert`) are parsed for correct arguments but they are not currently
exposed as an API, and therefore do nothing. The `:monitor` directive is parsed for correct arguments, but the format string is not yet verified. *jocto* will ignore
incorrect usage of or unknown format specifiers within the format string and not produce an assembler error.**

*jocto* runs the [c-octo-tests](https://github.com/gulrak/chiplet/tree/dev-road-to-2.0/test/c-octo-tests) test suite in *chiplet*'s repository, which includes *chiplet* specific tests and [c-octo's own tests](https://github.com/JohnEarnest/c-octo/tree/main/tests).
A list of current failures can be found in the [expected-failures.txt](https://github.com/ArkoSammy12/jocto/blob/master/core/src/test/resources/c-octo-tests/expected-failures.txt) file.

# References
- [John Earnest - Octo](https://github.com/JohnEarnest/Octo)  
- [John Earnest - c-octo](https://github.com/JohnEarnest/c-octo)
- [Timendus - chipcode](https://github.com/Timendus/chipcode)
- [Timendus - Octopus2](https://github.com/Timendus/chipcode/tree/main/octopus2)
- [Gulrak - chiplet](https://github.com/gulrak/chiplet)