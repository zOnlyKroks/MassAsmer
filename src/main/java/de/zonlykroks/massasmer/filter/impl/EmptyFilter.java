package de.zonlykroks.massasmer.filter.impl;

import de.zonlykroks.massasmer.filter.api.TransformerFilter;

/**
 * Predefined filters that match all or no classes.
 * These are implemented as singletons to avoid unnecessary object creation.
 */
public final class EmptyFilter implements TransformerFilter {
    // Private singleton instances
    private static final EmptyFilter MATCH_ALL = new EmptyFilter(true);
    private static final EmptyFilter MATCH_NONE = new EmptyFilter(false);

    private final boolean matchResult;

    private EmptyFilter(boolean matchResult) {
        this.matchResult = matchResult;
    }

    @Override
    public boolean matches(String className) {
        return matchResult;
    }

    /**
     * Returns a filter that matches all class names.
     *
     * @return A filter that always returns true
     */
    public static EmptyFilter matchAll() {
        return MATCH_ALL;
    }

    /**
     * Returns a filter that matches no class names.
     *
     * @return A filter that always returns false
     */
    public static EmptyFilter matchNone() {
        return MATCH_NONE;
    }
}