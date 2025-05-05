package de.zonlykroks.massasmer.filter;

import de.zonlykroks.massasmer.filter.api.TransformerFilter;
import de.zonlykroks.massasmer.filter.impl.*;

import java.lang.annotation.Annotation;
import java.util.regex.Pattern;

/**
 * Factory class providing static methods for creating and combining filters.
 */
public final class Filters {
    private Filters() {}

    /**
     * Creates a filter that matches all class names.
     *
     * @return A filter that always returns true
     */
    public static TransformerFilter all() {
        return EmptyFilter.matchAll();
    }

    /**
     * Creates a filter that matches no class names.
     *
     * @return A filter that always returns false
     */
    public static TransformerFilter none() {
        return EmptyFilter.matchNone();
    }

    /**
     * Creates a filter for exact class name matching.
     *
     * @param className The exact class name to match
     * @return A filter for exact name matching
     */
    public static TransformerFilter exact(String className) {
        return NamePatternFilter.exact(className);
    }

    /**
     * Creates a filter that matches class names starting with the given prefix.
     *
     * @param prefix The prefix to match
     * @return A filter for prefix matching
     */
    public static TransformerFilter startsWith(String prefix) {
        return NamePatternFilter.startsWith(prefix);
    }

    /**
     * Creates a filter that matches class names ending with the given suffix.
     *
     * @param suffix The suffix to match
     * @return A filter for suffix matching
     */
    public static TransformerFilter endsWith(String suffix) {
        return NamePatternFilter.endsWith(suffix);
    }

    /**
     * Creates a filter that matches class names containing the given substring.
     *
     * @param substring The substring to find within class names
     * @return A filter for substring matching
     */
    public static TransformerFilter contains(String substring) {
        return NamePatternFilter.contains(substring);
    }

    /**
     * Creates a filter that matches class names using a regular expression.
     *
     * @param regex The regular expression to match against
     * @return A filter for regex matching
     */
    public static TransformerFilter regex(String regex) {
        return new RegexFilter(regex);
    }

    /**
     * Creates a filter that matches class names using a regular expression.
     *
     * @param regex The regular expression to match against
     * @param flags Pattern compilation flags (see {@link Pattern})
     * @return A filter for regex matching
     */
    public static TransformerFilter regex(String regex, int flags) {
        return new RegexFilter(regex, flags);
    }

    /**
     * Creates a filter that matches classes with the specified annotation.
     *
     * @param annotationClass The annotation class to check for
     * @return A filter that matches classes with the given annotation
     */
    public static TransformerFilter hasAnnotation(Class<? extends Annotation> annotationClass) {
        return AnnotationFilter.hasAnnotation(annotationClass);
    }

    /**
     * Creates a filter that matches classes without the specified annotation.
     *
     * @param annotationClass The annotation class to check for absence
     * @return A filter that matches classes without the given annotation
     */
    public static TransformerFilter lacksAnnotation(Class<? extends Annotation> annotationClass) {
        return AnnotationFilter.lacksAnnotation(annotationClass);
    }

    /**
     * Creates a filter that matches classes implementing the specified interface.
     *
     * @param interfaceClass The interface class that must be implemented
     * @return A filter that matches classes implementing the given interface
     * @throws IllegalArgumentException if the provided class is not an interface
     */
    public static TransformerFilter implementsInterface(Class<?> interfaceClass) {
        return InterfaceImplementationFilter.implements_(interfaceClass);
    }

    /**
     * Creates a filter that matches classes not implementing the specified interface.
     *
     * @param interfaceClass The interface class that must not be implemented
     * @return A filter that matches classes not implementing the given interface
     * @throws IllegalArgumentException if the provided class is not an interface
     */
    public static TransformerFilter doesNotImplementInterface(Class<?> interfaceClass) {
        return InterfaceImplementationFilter.doesNotImplement(interfaceClass);
    }

    /**
     * Creates a filter that matches classes extending the specified superclass.
     *
     * @param superClass The superclass that must be extended
     * @return A filter that matches classes extending the given superclass
     * @throws IllegalArgumentException if the provided class is an interface
     */
    public static TransformerFilter extendsClass(Class<?> superClass) {
        return SuperclassFilter.extends_(superClass);
    }

    /**
     * Creates a filter that matches classes not extending the specified superclass.
     *
     * @param superClass The superclass that must not be extended
     * @return A filter that matches classes not extending the given superclass
     * @throws IllegalArgumentException if the provided class is an interface
     */
    public static TransformerFilter doesNotExtendClass(Class<?> superClass) {
        return SuperclassFilter.doesNotExtend(superClass);
    }

    /**
     * Combines two filters with logical AND.
     *
     * @param left The first filter
     * @param right The second filter
     * @return A filter that matches when both input filters match
     */
    public static TransformerFilter and(TransformerFilter left, TransformerFilter right) {
        return CompositeFilter.and(left, right);
    }

    /**
     * Combines two filters with logical OR.
     *
     * @param left The first filter
     * @param right The second filter
     * @return A filter that matches when either input filter matches
     */
    public static TransformerFilter or(TransformerFilter left, TransformerFilter right) {
        return CompositeFilter.or(left, right);
    }

    /**
     * Creates a new filter that negates the input filter.
     *
     * @param filter The filter to negate
     * @return A filter that matches when the input filter does not match
     */
    public static TransformerFilter not(TransformerFilter filter) {
        return CompositeFilter.not(filter);
    }
}