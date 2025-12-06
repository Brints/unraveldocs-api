package com.extractor.unraveldocs.admin.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreatedEvent implements Serializable {
    private String id;
    private String email;
    private String token;
    private String firstName;
    private String lastName;
    private String tokenExpiry;
}
