package com.extractor.unraveldocs;

import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class RootController {
    @GetMapping("/")
    public ResponseEntity<UnravelDocsDataResponse<Void>> rootEndpoint() {
        UnravelDocsDataResponse<Void> response = new UnravelDocsDataResponse<>(
                200,
                "success",
                "UnravelDocs API is running." + " Current Server: " + Instant.now().toString(),
                null
        );
        return ResponseEntity.ok(response);
    }
}
