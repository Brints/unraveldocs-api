package com.extractor.unraveldocs.admin.dto.request;

import com.extractor.unraveldocs.auth.datamodel.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRoleDto {
    private String userId;
    private Role role;
}
