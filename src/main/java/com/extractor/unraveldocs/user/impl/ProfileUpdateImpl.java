package com.extractor.unraveldocs.user.impl;

import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.user.dto.UserData;
import com.extractor.unraveldocs.user.dto.request.ProfileUpdateRequestDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.interfaces.userimpl.ProfileUpdateService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.elasticsearch.events.IndexAction;
import com.extractor.unraveldocs.elasticsearch.service.ElasticsearchIndexingService;
import com.extractor.unraveldocs.utils.userlib.UserLibrary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.extractor.unraveldocs.shared.response.ResponseData.getResponseData;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileUpdateImpl implements ProfileUpdateService {
    private final ResponseBuilderService responseBuilder;
    private final UserLibrary userLibrary;
    private final UserRepository userRepository;
    private final Optional<ElasticsearchIndexingService> elasticsearchIndexingService;

    @Override
    @Transactional
    @CachePut(value = { "getProfileByUser", "getProfileByAdmin" }, key = "#userId")
    public UnravelDocsResponse<UserData> updateProfile(ProfileUpdateRequestDto request, String userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new NotFoundException("User not found with ID: " + userId);
        }

        User user = optionalUser.get();
        boolean hasChanges = false;

        // Update firstName if provided and different
        if (request.firstName() != null && !request.firstName().isBlank()
                && !request.firstName().equalsIgnoreCase(user.getFirstName())) {
            String capitalizedFirstName = userLibrary.capitalizeFirstLetterOfName(request.firstName());
            user.setFirstName(capitalizedFirstName);
            hasChanges = true;
            log.debug("Updated firstName for user {}", userId);
        }

        // Update lastName if provided and different
        if (request.lastName() != null && !request.lastName().isBlank()
                && !request.lastName().equalsIgnoreCase(user.getLastName())) {
            String capitalizedLastName = userLibrary.capitalizeFirstLetterOfName(request.lastName());
            user.setLastName(capitalizedLastName);
            hasChanges = true;
            log.debug("Updated lastName for user {}", userId);
        }

        // Update country if provided and different
        if (request.country() != null && !request.country().isBlank()
                && !request.country().equals(user.getCountry())) {
            user.setCountry(request.country());
            hasChanges = true;
            log.debug("Updated country for user {}", userId);
        }

        // Update profession if provided and different
        if (request.profession() != null && !request.profession().isBlank()) {
            if (user.getProfession() == null || !request.profession().equals(user.getProfession())) {
                user.setProfession(request.profession());
                hasChanges = true;
                log.debug("Updated profession for user {}", userId);
            }
        }

        // Update organization if provided and different
        if (request.organization() != null && !request.organization().isBlank()) {
            if (user.getOrganization() == null || !request.organization().equals(user.getOrganization())) {
                user.setOrganization(request.organization());
                hasChanges = true;
                log.debug("Updated organization for user {}", userId);
            }
        }

        User updatedUser;
        if (hasChanges) {
            updatedUser = userRepository.save(user);
            log.info("Profile updated successfully for user {}", userId);

            // Index updated user in Elasticsearch
            elasticsearchIndexingService.ifPresent(service -> service.indexUser(updatedUser, IndexAction.UPDATE));
        } else {
            updatedUser = user;
            log.info("No changes detected for user {}", userId);
        }

        UserData data = getResponseData(updatedUser, UserData::new);

        return responseBuilder.buildUserResponse(data, HttpStatus.OK, "Profile updated successfully");
    }
}
