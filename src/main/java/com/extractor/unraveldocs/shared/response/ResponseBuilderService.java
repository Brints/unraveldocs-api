package com.extractor.unraveldocs.shared.response;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ResponseBuilderService {
    public <T> UnravelDocsResponse<T> buildUserResponse(T data, HttpStatus statusCode, String message) {
        UnravelDocsResponse<T> unravelDocsResponse = new UnravelDocsResponse<>();
        unravelDocsResponse.setStatusCode(statusCode.value());
        unravelDocsResponse.setStatus("success");
        unravelDocsResponse.setMessage(message);
        unravelDocsResponse.setData(data);

        return unravelDocsResponse;
    }
}
