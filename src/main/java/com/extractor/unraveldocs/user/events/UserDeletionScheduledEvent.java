package com.extractor.unraveldocs.user.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeletionScheduledEvent implements Serializable {
    private String email;
    private String firstName;
    private String lastName;
    private OffsetDateTime deletionDate; // ISO 8601 format
    private String reason; // Optional reason for deletion
}
