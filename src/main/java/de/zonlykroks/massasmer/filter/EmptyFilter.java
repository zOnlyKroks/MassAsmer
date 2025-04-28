package de.zonlykroks.massasmer.filter;

// Empty filter that matches all classes
public class EmptyFilter implements TransformerFilter {
    @Override
    public boolean matches(String className) {
        return true;  // Always true, matches all classes
    }

    @Override
    public boolean test(String className) {
        return true;  // Matches all classes
    }
}
