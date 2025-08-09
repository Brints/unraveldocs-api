package com.extractor.unraveldocs.user.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeletedEvent implements Serializable {
    private String email;
    private String profilePictureUrl;
}
