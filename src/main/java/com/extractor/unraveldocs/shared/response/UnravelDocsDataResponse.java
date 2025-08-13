package com.extractor.unraveldocs.shared.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnravelDocsDataResponse<T> {
    int statusCode;
    String status;
    String message;
    T data;
}
