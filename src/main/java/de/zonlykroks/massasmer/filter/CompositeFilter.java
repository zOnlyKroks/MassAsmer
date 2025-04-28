package de.zonlykroks.massasmer.filter;

// Composite filter that combines two filters with AND/OR logic
public class CompositeFilter implements TransformerFilter {
    private final TransformerFilter filter1;
    private final TransformerFilter filter2;
    private final boolean andOperation; // true for AND, false for OR

    public CompositeFilter(TransformerFilter filter1, TransformerFilter filter2, boolean andOperation) {
        this.filter1 = filter1;
        this.filter2 = filter2;
        this.andOperation = andOperation;
    }

    @Override
    public boolean test(String className) {
        return andOperation ? filter1.test(className) && filter2.test(className)
                : filter1.test(className) || filter2.test(className);
    }

    @Override
    public boolean matches(String className) {
        return test(className);
    }
}
