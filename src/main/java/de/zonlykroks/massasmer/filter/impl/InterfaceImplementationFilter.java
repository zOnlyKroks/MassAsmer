package de.zonlykroks.massasmer.filter.impl;

import de.zonlykroks.massasmer.filter.api.TransformerFilter;

/**
 * Filter that matches classes based on interface implementation.
 * This filter checks if a class implements a specific interface or not.
 */
public record InterfaceImplementationFilter(Class<?> interfaceClass,
                                            boolean requireImplementation) implements TransformerFilter {
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

}