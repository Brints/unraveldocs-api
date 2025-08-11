package com.extractor.unraveldocs.user.events;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class PasswordResetRequestedEvent implements Serializable {
    private String email;
    private String firstName;
    private String lastName;
    private String token;
    private String expiration;
}