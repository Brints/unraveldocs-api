package com.extractor.unraveldocs.exceptions.custom;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {
    private final String errorCode;

    public BadRequestException(String message) {
        super(message);
        this.errorCode = null;
    }

    public BadRequestException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
