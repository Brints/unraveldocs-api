package com.extractor.unraveldocs.exceptions.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private int statusCode;
    private String error;
    private String message;
    private String errorCode;
}
