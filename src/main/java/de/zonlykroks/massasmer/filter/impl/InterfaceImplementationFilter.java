package de.zonlykroks.massasmer.filter.impl;

import de.zonlykroks.massasmer.filter.api.TransformerFilter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * Filter that matches classes based on interface implementation.
 * This filter checks if a class implements a specific interface or not.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class InterfaceImplementationFilter implements TransformerFilter {
    private final Class<?> interfaceClass;
    private final boolean requireImplementation;

    /**
     * Creates a filter that matches classes implementing the specified interface.
     *
     * @param interfaceClass The interface class that must be implemented
     * @return A new filter that matches classes implementing the given interface
     * @throws IllegalArgumentException if the provided class is not an interface
     */
    public static InterfaceImplementationFilter implements_(Class<?> interfaceClass) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("The provided class must be an interface: " + interfaceClass.getName());
        }
        return new InterfaceImplementationFilter(interfaceClass, true);
    }

    /**
     * Creates a filter that matches classes NOT implementing the specified interface.
     *
     * @param interfaceClass The interface class that must not be implemented
     * @return A new filter that matches classes not implementing the given interface
     * @throws IllegalArgumentException if the provided class is not an interface
     */
    public static InterfaceImplementationFilter doesNotImplement(Class<?> interfaceClass) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("The provided class must be an interface: " + interfaceClass.getName());
        }
        return new InterfaceImplementationFilter(interfaceClass, false);
    }

    @Override
    public boolean matches(String className) {
        if (className == null || interfaceClass == null) {
            return false;
        }

        try {
            Class<?> clazz = Class.forName(className);
            boolean implementsInterface = interfaceClass.isAssignableFrom(clazz);
            return requireImplementation == implementsInterface;
        } catch (ClassNotFoundException e) {
            // If the class cannot be loaded, it can't be matched
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterfaceImplementationFilter that = (InterfaceImplementationFilter) o;
        return requireImplementation == that.requireImplementation &&
                Objects.equals(interfaceClass, that.interfaceClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interfaceClass, requireImplementation);
    }
}