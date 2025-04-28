package de.zonlykroks.massasmer.filter;

import java.util.function.Predicate;
import java.util.regex.Pattern;

// Base interface for filters
public interface TransformerFilter extends Predicate<String> {
    boolean matches(String className);
}

