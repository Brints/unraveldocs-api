package com.extractor.unraveldocs.admin.impl;

import com.extractor.unraveldocs.admin.dto.AdminData;
import com.extractor.unraveldocs.admin.dto.request.ChangeRoleDto;
import com.extractor.unraveldocs.admin.interfaces.ChangeUserRoleService;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.exceptions.custom.UnauthorizedException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.impl.AssignSubscriptionService;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import static com.extractor.unraveldocs.shared.response.ResponseData.getResponseData;

@Service
@RequiredArgsConstructor
public class ChangeUserRoleImpl implements ChangeUserRoleService {
    private final AssignSubscriptionService assignSubscriptionService;
    private final UserRepository userRepository;
    private final ResponseBuilderService responseBuilder;

    @Override
    public UnravelDocsResponse<AdminData> changeUserRole(ChangeRoleDto request, Authentication authentication) {
        String userId = request.getUserId();

        // Admin or Super Admin check
        if (authentication.getAuthorities().stream()
                .noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_admin") ||
                        grantedAuthority.getAuthority().equals("ROLE_super_admin"))) {
            throw new UnauthorizedException("You must be an admin or super admin to change user roles");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        if (!user.isVerified()) {
            throw new ForbiddenException("Cannot change role for unverified users");
        }

        user.setRole(request.getRole());

        UserSubscription subscription = assignSubscriptionService.assignDefaultSubscription(user);
        user.setSubscription(subscription);

        userRepository.save(user);


        AdminData data = getResponseData(user, AdminData::new);

        return responseBuilder.buildUserResponse(
                data,
                HttpStatus.OK,
                "User role changed successfully."
        );
    }
}
