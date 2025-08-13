package com.extractor.unraveldocs.shared.response;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ResponseBuilderService {
    public <T> UnravelDocsDataResponse<T> buildUserResponse(T data, HttpStatus statusCode, String message) {
        UnravelDocsDataResponse<T> unravelDocsDataResponse = new UnravelDocsDataResponse<>();
        unravelDocsDataResponse.setStatusCode(statusCode.value());
        unravelDocsDataResponse.setStatus("success");
        unravelDocsDataResponse.setMessage(message);
        unravelDocsDataResponse.setData(data);

        return unravelDocsDataResponse;
    }
}
