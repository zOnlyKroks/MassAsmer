package de.zonlykroks.massasmer.api;

import de.zonlykroks.massasmer.filter.api.TransformerFilter;

/**
 * Represents a registered transformer in the MassASM system.
 * Provides access to transformer metadata and control over its execution.
 */
public interface TransformerRegistration {
    /**
     * Gets the unique name of this transformer.
     *
     * @return The transformer name
     */
    String name();

    /**
     * Gets the filter used by this transformer to match classes.
     *
     * @return The transformer filter
     */
    TransformerFilter filter();

    /**
     * Gets the priority of this transformer.
     * Higher priority transformers execute first.
     *
     * @return The priority value (default is 0)
     */
    int priority();

    /**
     * Checks if this transformer is currently enabled.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Disables this transformer, preventing it from being applied to classes.
     * Note: This marks the transformer as disabled but does not remove it from the registry.
     */
    void disable();

    /**
     * Enables this transformer, allowing it to be applied to classes again.
     */
    void enable();
}
