package com.extractor.unraveldocs;

import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class RootController {
    @GetMapping("/")
    public ResponseEntity<UnravelDocsResponse<String>> rootEndpoint() {

        UnravelDocsResponse<String> response = new UnravelDocsResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setStatus("success");
        response.setMessage("UnravelDocs API is running");
        response.setData("Current server time: " + Instant.now().toString());

        return ResponseEntity.ok(response);
    }
}
