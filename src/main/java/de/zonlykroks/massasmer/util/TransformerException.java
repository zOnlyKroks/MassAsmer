package de.zonlykroks.massasmer.util;

import lombok.Getter;

/**
 * Exception thrown when a transformer fails to process a class.
 * Provides context about which transformer failed and which class was being processed.
 */
@Getter
public class TransformerException extends RuntimeException {
    private final String transformerName;
    private final String className;

    public TransformerException(String transformerName, String className, Throwable cause) {
        super(String.format("Transformer '%s' failed while processing class '%s'", transformerName, className), cause);
        this.transformerName = transformerName;
        this.className = className;
    }

}
