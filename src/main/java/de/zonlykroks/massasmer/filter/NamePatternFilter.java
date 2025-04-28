package de.zonlykroks.massasmer.filter;

import java.util.Objects;

// Filter for string-based matching on class names using native string operations
public class NamePatternFilter implements TransformerFilter {
    private final String pattern;
    private final boolean startsWith;
    private final boolean endsWith;
    private final boolean contains;
    private final boolean exactContentMatch;

    // Constructor for simple pattern matching
    public NamePatternFilter(String pattern) {
        this.pattern = pattern;
        this.exactContentMatch = true;
        this.startsWith = false;
        this.endsWith = false;
        this.contains = false;
    }

    // Constructor for 'startsWith' matching
    public NamePatternFilter(String pattern, boolean exactContentMatch,boolean startsWith, boolean endsWith, boolean contains) {
        this.pattern = pattern;
        this.startsWith = startsWith;
        this.endsWith = endsWith;
        this.contains = contains;
        this.exactContentMatch = exactContentMatch;
    }

    @Override
    public boolean matches(String className) {
        if(exactContentMatch) {
            return className.equals(pattern);
        }

        if (startsWith) {
            return className.startsWith(pattern);
        }
        if (endsWith) {
            return className.endsWith(pattern);
        }
        if (contains) {
            return className.contains(pattern);
        }

        // Fallback to exact matching
        return Objects.equals(className, pattern);
    }

    @Override
    public boolean test(String className) {
        return matches(className);
    }
}
