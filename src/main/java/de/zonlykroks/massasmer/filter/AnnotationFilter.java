package de.zonlykroks.massasmer.filter;

// Filter for checking if a class has a specific annotation (future extensibility)
public class AnnotationFilter implements TransformerFilter {
    private final String annotationClassName;

    public AnnotationFilter(String annotationClassName) {
        this.annotationClassName = annotationClassName;
    }

    @Override
    public boolean matches(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.isAnnotationPresent(Class.forName(annotationClassName).asSubclass(java.lang.annotation.Annotation.class));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean test(String className) {
        return matches(className);
    }
}
