package de.zonlykroks.massasmer.filter.impl;

import de.zonlykroks.massasmer.filter.api.TransformerFilter;
import lombok.Getter;

import java.util.regex.Pattern;

/**
 * Filter for regex-based matching on class names.
 * Provides more powerful pattern matching capabilities than simple string operations.
 * Be warned, this is dead slow, just on the premise on how regexés work.
 */
@Getter
public class RegexFilter implements TransformerFilter {
    private final Pattern pattern;
    private final String patternString;

    /**
     * Creates a new filter with the given regex pattern.
     *
     * @param regex The regular expression to match against class names
     */
    public RegexFilter(String regex) {
        this.patternString = regex;
        this.pattern = Pattern.compile(regex);
    }

    /**
     * Creates a new filter with the given regex pattern and compilation flags.
     *
     * @param regex The regular expression to match against class names
     * @param flags Pattern compilation flags (see {@link Pattern})
     */
    public RegexFilter(String regex, int flags) {
        this.patternString = regex;
        this.pattern = Pattern.compile(regex, flags);
    }

    @Override
    public boolean matches(String className) {
        if (className == null) {
            return false;
        }
        return pattern.matcher(className).matches();
    }
}