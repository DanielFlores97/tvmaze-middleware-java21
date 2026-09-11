package com.example.pruebatecnica.domain;

public class AppException extends RuntimeException {
    public enum Code {
        POST_NOT_FOUND, UPSTREAM_UNAVAILABLE, UPSTREAM_TIMEOUT, CACHE_UNAVAILABLE, INVALID_REQUEST
    }

    private final Code code;

    public AppException(Code code) {
        super(code.name());
        this.code = code;
    }

    public AppException(Code code, Throwable cause) {
        super(code.name(), cause);
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
