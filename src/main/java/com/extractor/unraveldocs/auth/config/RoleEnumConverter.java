package com.extractor.unraveldocs.auth.config;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RoleEnumConverter implements Converter<String, Role> {
    @Override
    public Role convert(String source) {
        try {
            return Role.fromString(source.trim());
        } catch (BadRequestException e) {
            throw new BadRequestException("[" + source + "] is not a valid enum. Valid values are: " + String.join(", ", Role.getValidRoles()));
        }
    }
}
