package com.extractor.unraveldocs.user.events;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class PasswordChangedEvent implements Serializable {
    private String email;
    private String firstName;
    private String lastName;
}