package com.extractor.unraveldocs.auth.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeEvent implements Serializable {
    private String email;
    private String firstName;
    private String lastName;
}
