package de.zonlykroks.massasmer.filter.impl;

import de.zonlykroks.massasmer.filter.api.TransformerFilter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A string-based filter for matching class names using different strategies.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class NamePatternFilter implements TransformerFilter {
    private final String pattern;
    private final MatchStrategy strategy;

    /**
     * Strategy enum defining how the pattern should be matched
     */
    public enum MatchStrategy {
        EXACT,
        STARTS_WITH,
        ENDS_WITH,
        CONTAINS
    }

    /**
     * Tests if the given class name matches according to the configured pattern and strategy.
     *
     * @param className The class name to test
     * @return true if the class name matches the pattern, false otherwise
     */
    @Override
    public boolean matches(String className) {
        if (className == null || pattern == null) {
            return false;
        }

        return switch (strategy) {
            case EXACT -> className.equals(pattern);
            case STARTS_WITH -> className.startsWith(pattern);
            case ENDS_WITH -> className.endsWith(pattern);
            case CONTAINS -> className.contains(pattern);
        };
    }

    // Static factory methods for creating filters with different strategies

    /**
     * Creates a filter that matches class names exactly equal to the pattern.
     *
     * @param pattern The exact string to match
     * @return A new filter for exact matching
     */
    public static NamePatternFilter exact(String pattern) {
        return new NamePatternFilter(pattern, MatchStrategy.EXACT);
    }

    /**
     * Creates a filter that matches class names starting with the pattern.
     *
     * @param pattern The prefix to match at the start of class names
     * @return A new filter for prefix matching
     */
    public static NamePatternFilter startsWith(String pattern) {
        return new NamePatternFilter(pattern, MatchStrategy.STARTS_WITH);
    }

    /**
     * Creates a filter that matches class names ending with the pattern.
     *
     * @param pattern The suffix to match at the end of class names
     * @return A new filter for suffix matching
     */
    public static NamePatternFilter endsWith(String pattern) {
        return new NamePatternFilter(pattern, MatchStrategy.ENDS_WITH);
    }

    /**
     * Creates a filter that matches class names containing the pattern.
     *
     * @param pattern The substring to find within class names
     * @return A new filter for substring matching
     */
    public static NamePatternFilter contains(String pattern) {
        return new NamePatternFilter(pattern, MatchStrategy.CONTAINS);
    }
}