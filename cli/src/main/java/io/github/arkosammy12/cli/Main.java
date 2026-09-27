package io.github.arkosammy12.cli;

import io.github.arkosammy12.core.util.Position;
import io.github.arkosammy12.core.util.SourceFile;

import java.util.List;

public class Main {

    static void main(String[] args) {
        String test = """
                0: 
                1: Hello guys!!!
                2:                 
                3: hello people # this is a commena
                4: 
                5: # this is another commena
                6: 
                7: this is # a comment
                8: 
                """;

        SourceFile sourceFile = new SourceFile(List.of(test.split("\n")));

        IO.println("Original: \n" + sourceFile);

        Position numeralPosition;
        while ((numeralPosition = sourceFile.indexOf("#").orElse(null)) != null) {
            sourceFile = sourceFile.remove(numeralPosition.row(), numeralPosition.column(), numeralPosition.row());
        }

        IO.println("\nModified: \n" + sourceFile);
    }

}
