package io.github.arkosammy12.core.lexer;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SourceStream<T> {

    private final Deque<T> streamDeque = new ArrayDeque<>();

    public SourceStream(SourceStream<T> other) {
        this.streamDeque.addAll(other.streamDeque);
    }

    public SourceStream(Collection<T> elements) {
        this.streamDeque.addAll(elements);
    }

    public static SourceStream<SourceCharacter> ofLines(Collection<String> lines) {
        List<SourceCharacter> sourceCharacters = new ArrayList<>();
        int row = 0;
        for (String line : lines) {
            for (int column = 0; column < line.length(); column++) {
                sourceCharacters.add(new SourceCharacter(line.charAt(column), new SourcePosition(row, column)));
            }
            row++;
        }
        return new SourceStream<>(sourceCharacters);
    }

    public boolean isEmpty() {
        return this.streamDeque.isEmpty();
    }

    public void offerFront(T value) {
        this.streamDeque.offerFirst(value);
    }

    public void forEach(Consumer<T> consumer) {
        for (T t : this.streamDeque) {
            consumer.accept(t);
        }
    }

    public Optional<T> poll() {
        if (this.streamDeque.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(this.streamDeque.poll());
        }
    }

    public Collection<T> pollUntil(Predicate<T> characterPredicate) {
        Collection<T> characters = new ArrayList<>();
        while (!this.peek().map(characterPredicate::test).orElse(true)) {
            this.poll().ifPresent(characters::add);
        }
        return characters;
    }

    public Optional<T> peek() {
        if (this.streamDeque.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(this.streamDeque.peek());
        }
    }

    public Optional<Collection<T>> peekUntil(Predicate<T> characterPredicate) {
        Collection<T> characters = new ArrayList<>();

        for (T sourceCharacter : this.streamDeque) {
            if (characterPredicate.test(sourceCharacter)) {
                break;
            } else {
                characters.add(sourceCharacter);
            }
        }

        if (characters.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(characters);
        }
    }

}
