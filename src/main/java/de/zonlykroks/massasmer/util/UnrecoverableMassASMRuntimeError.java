package de.zonlykroks.massasmer.util;

public class UnrecoverableMassASMRuntimeError extends RuntimeException{
    public UnrecoverableMassASMRuntimeError(String s, Throwable throwable) {
        super(s, throwable);
    }
}
