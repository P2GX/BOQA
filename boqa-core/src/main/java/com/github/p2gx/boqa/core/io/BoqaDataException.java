package com.github.p2gx.boqa.core.io;

/**
 * An exception thrown when data error (missing resource, invalid resource file, etc.) is detected.
 */
public class BoqaDataException extends Exception {

    public BoqaDataException() {
        super();
    }

    public BoqaDataException(String message) {
        super(message);
    }

    public BoqaDataException(String message, Exception e) {
        super(message, e);
    }

    public BoqaDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public BoqaDataException(Throwable cause) {
        super(cause);
    }

    protected BoqaDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}