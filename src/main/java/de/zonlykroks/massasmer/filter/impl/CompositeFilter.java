package de.zonlykroks.massasmer.filter.impl;

import de.zonlykroks.massasmer.filter.api.TransformerFilter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Combines two filters with logical operations (AND, OR, NOT).
 * Used to build complex filtering expressions by combining simpler filters.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CompositeFilter implements TransformerFilter {
    private final TransformerFilter left;
    private final TransformerFilter right;
    private final LogicalOperation operation;

    /**
     * Logical operations that can be applied to filters
     */
    public enum LogicalOperation {
        AND,
        OR,
        NOT
    }

    @Override
    public boolean matches(String className) {
        return switch (operation) {
            case AND -> left.matches(className) && right.matches(className);
            case OR -> left.matches(className) || right.matches(className);
            case NOT -> !left.matches(className);
        };
    }

    /**
     * Creates a composite filter with logical AND between two filters.
     *
     * @param left The first filter
     * @param right The second filter
     * @return A new composite filter that matches when both filters match
     */
    public static CompositeFilter and(TransformerFilter left, TransformerFilter right) {
        return new CompositeFilter(left, right, LogicalOperation.AND);
    }

    /**
     * Creates a composite filter with logical OR between two filters.
     *
     * @param left The first filter
     * @param right The second filter
     * @return A new composite filter that matches when either filter matches
     */
    public static CompositeFilter or(TransformerFilter left, TransformerFilter right) {
        return new CompositeFilter(left, right, LogicalOperation.OR);
    }

    /**
     * Creates a composite filter that negates another filter.
     *
     * @param filter The filter to negate
     * @return A new composite filter that matches when the input filter does not match
     */
    public static CompositeFilter not(TransformerFilter filter) {
        // Using EmptyFilter.matchAll() as a placeholder since NOT only needs one filter
        return new CompositeFilter(filter, EmptyFilter.matchAll(), LogicalOperation.NOT);
    }
}