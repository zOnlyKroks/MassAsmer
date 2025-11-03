package de.zonlykroks.massasmer.filter.impl;

import de.zonlykroks.massasmer.filter.api.TransformerFilter;

import java.lang.annotation.Annotation;

/**
 * Filter that matches classes based on the presence of specific annotations.
 * This filter allows filtering classes that are annotated with particular annotations.
 */
public record AnnotationFilter(Class<? extends Annotation> annotationClass,
                               boolean requireAnnotation) implements TransformerFilter {
    /**
     * Creates a filter that matches classes annotated with the specified annotation.
     *
     * @param annotationClass The annotation class to check for
     * @return A new filter that matches classes with the given annotation
     */
    public static AnnotationFilter hasAnnotation(Class<? extends Annotation> annotationClass) {
        return new AnnotationFilter(annotationClass, true);
    }

    /**
     * Creates a filter that matches classes NOT annotated with the specified annotation.
     *
     * @param annotationClass The annotation class to check for absence
     * @return A new filter that matches classes without the given annotation
     */
    public static AnnotationFilter lacksAnnotation(Class<? extends Annotation> annotationClass) {
        return new AnnotationFilter(annotationClass, false);
    }

    @Override
    public boolean matches(String className) {
        if (className == null || annotationClass == null) {
            return false;
        }

        try {
            Class<?> clazz = Class.forName(className);
            boolean hasAnnotation = clazz.isAnnotationPresent(annotationClass);
            return requireAnnotation == hasAnnotation;
        } catch (ClassNotFoundException e) {
            // If the class cannot be loaded, it can't be matched
            return false;
        }
    }

}