package com.extractor.unraveldocs.admin.impl;

import com.extractor.unraveldocs.admin.dto.AdminData;
import com.extractor.unraveldocs.admin.dto.request.CreateAdminRequestDto;
import com.extractor.unraveldocs.admin.interfaces.CreateAdminService;
import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.events.EventPublisherService;
import com.extractor.unraveldocs.exceptions.custom.ConflictException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.impl.AssignSubscriptionService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import com.extractor.unraveldocs.utils.userlib.DateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAdminUserImpl implements CreateAdminService {
    private final AssignSubscriptionService subscription;
    private final DateHelper dateHelper;
    private final EventPublisherService eventPublisher;
    private final GenerateVerificationToken generateVerificationToken;
    private final ResponseBuilderService responseBuilder;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UnravelDocsResponse<AdminData> createAdminUser(CreateAdminRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Admin user already exists with email: " + request.getEmail());
        }

        if (request.getPassword().equalsIgnoreCase(request.getEmail())) {
            throw new ConflictException("Password cannot be same as email.");
        }

        User newAdmin = new User();
        newAdmin.setFirstName(request.getFirstName());
        newAdmin.setLastName(request.getLastName());
        newAdmin.setEmail(request.getEmail());
        newAdmin.setPassword(request.getPassword());
        newAdmin.setActive(true);
        newAdmin.setVerified(true);
        newAdmin.setRole(Role.ADMIN);
        userRepository.save(newAdmin);

        subscription.assignDefaultSubscription(newAdmin);
        AdminData adminData = new AdminData();
        adminData.setEmail(newAdmin.getEmail());
        adminData.setFirstName(newAdmin.getFirstName());
        adminData.setLastName(newAdmin.getLastName());
        adminData.setRole(Role.ADMIN);
        return responseBuilder.buildUserResponse(adminData, HttpStatus.CREATED, "Admin user created successfully");
    }
}
