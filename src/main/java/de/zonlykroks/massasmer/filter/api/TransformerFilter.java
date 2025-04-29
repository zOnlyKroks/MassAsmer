package de.zonlykroks.massasmer.filter.api;

import de.zonlykroks.massasmer.filter.Filters;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;


public interface TransformerFilter extends Predicate<String> {
    /**
     * Tests if the given class name matches this filter's criteria.
     *
     * @param className The fully qualified class name to test
     * @return true if the class matches, false otherwise
     */
    boolean matches(String className);

    /**
     * Implementation of Predicate's test method.
     * By default, delegates to matches() for consistency.
     *
     * @param className The class name to test
     * @return true if the class matches, false otherwise
     */
    @Override
    default boolean test(String className) {
        return matches(className);
    }

    /**
     * Creates a new filter that is the logical AND of this filter and another.
     *
     * @param other The filter to AND with this one
     * @return A new composite filter representing this AND other
     */
    default TransformerFilter and(TransformerFilter other) {
        return Filters.and(this, other);
    }

    /**
     * Creates a new filter that is the logical OR of this filter and another.
     *
     * @param other The filter to OR with this one
     * @return A new composite filter representing this OR other
     */
    default TransformerFilter or(TransformerFilter other) {
        return Filters.or(this, other);
    }

    /**
     * Creates a new filter that is the logical negation of this filter.
     *
     * @return A new filter that matches when this filter does not match
     */
    default @NotNull TransformerFilter negate() {
        return Filters.not(this);
    }
}