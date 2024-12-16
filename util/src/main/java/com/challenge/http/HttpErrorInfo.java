package com.challenge.http;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

public class HttpErrorInfo {
    private final String path;
    private final int status;
    private final String error;
    private final String message;

    public HttpErrorInfo(String path, int status, String error, String message) {
        this.path = path;
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }
}