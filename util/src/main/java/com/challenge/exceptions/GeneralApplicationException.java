package com.challenge.exceptions;

public class GeneralApplicationException extends RuntimeException {
    public GeneralApplicationException() {}

    public GeneralApplicationException(String message) {
        super(message);
    }

    public GeneralApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeneralApplicationException(Throwable cause) {
        super(cause);
    }
}
