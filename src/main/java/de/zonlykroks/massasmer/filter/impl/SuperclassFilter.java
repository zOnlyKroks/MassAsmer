package de.zonlykroks.massasmer.filter.impl;

import de.zonlykroks.massasmer.filter.api.TransformerFilter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * Filter that matches classes based on their inheritance hierarchy.
 * This filter checks if a class extends a specific superclass.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SuperclassFilter implements TransformerFilter {
    private final Class<?> superClass;
    private final boolean requireExtension;

    /**
     * Creates a filter that matches classes extending the specified superclass.
     *
     * @param superClass The superclass that must be extended
     * @return A new filter that matches classes extending the given superclass
     * @throws IllegalArgumentException if the provided class is an interface
     */
    public static SuperclassFilter extends_(Class<?> superClass) {
        if (superClass.isInterface()) {
            throw new IllegalArgumentException("The provided class must not be an interface: " + superClass.getName());
        }
        return new SuperclassFilter(superClass, true);
    }

    /**
     * Creates a filter that matches classes NOT extending the specified superclass.
     *
     * @param superClass The superclass that must not be extended
     * @return A new filter that matches classes not extending the given superclass
     * @throws IllegalArgumentException if the provided class is an interface
     */
    public static SuperclassFilter doesNotExtend(Class<?> superClass) {
        if (superClass.isInterface()) {
            throw new IllegalArgumentException("The provided class must not be an interface: " + superClass.getName());
        }
        return new SuperclassFilter(superClass, false);
    }

    @Override
    public boolean matches(String className) {
        if (className == null || superClass == null) {
            return false;
        }

        try {
            Class<?> clazz = Class.forName(className);

            // Check if the class is the same as the superclass
            if (clazz.equals(superClass)) {
                return false; // A class doesn't extend itself
            }

            boolean extendsClass = superClass.isAssignableFrom(clazz);
            return requireExtension == extendsClass;
        } catch (ClassNotFoundException e) {
            // If the class cannot be loaded, it can't be matched
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuperclassFilter that = (SuperclassFilter) o;
        return requireExtension == that.requireExtension &&
                Objects.equals(superClass, that.superClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(superClass, requireExtension);
    }
}