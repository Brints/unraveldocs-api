package com.extractor.unraveldocs.auth.impl;

import com.extractor.unraveldocs.auth.dto.request.ResendEmailVerificationDto;
import com.extractor.unraveldocs.auth.datamodel.VerifiedStatus;
import com.extractor.unraveldocs.auth.interfaces.EmailVerificationService;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.messaging.emailtemplates.AuthEmailTemplateService;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import com.extractor.unraveldocs.utils.userlib.DateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationImpl implements EmailVerificationService {
    private final UserRepository userRepository;
    private final GenerateVerificationToken verificationToken;
    private final DateHelper dateHelper;
    private final AuthEmailTemplateService templatesService;
    private final ResponseBuilderService responseBuilder;

    @Override
    public UnravelDocsDataResponse<Void> resendEmailVerification(ResendEmailVerificationDto request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new NotFoundException("User does not exist."));

        if (user.isVerified()) {
            throw new BadRequestException("User is already verified. Please login.");
        }

        // Check if the user already has an active verification token
        UserVerification userVerification = user.getUserVerification();
        OffsetDateTime now = OffsetDateTime.now();
        if (userVerification.getEmailVerificationToken() != null) {
            String timeLeft = dateHelper.getTimeLeftToExpiry(now, userVerification.getEmailVerificationTokenExpiry(),
                    "hour");
            throw new BadRequestException(
                    "A verification email has already been sent. Token expires in: " + timeLeft);
        }

        String emailVerificationToken = verificationToken.generateVerificationToken();
        OffsetDateTime emailVerificationTokenExpiry = dateHelper.setExpiryDate(now, "hour", 3);

        userVerification.setEmailVerificationToken(emailVerificationToken);
        userVerification.setEmailVerificationTokenExpiry(emailVerificationTokenExpiry);
        userVerification.setStatus(VerifiedStatus.PENDING);
        userVerification.setEmailVerified(false);

        userRepository.save(user);

        // TODO: Send email with the verification token (implementation not shown)
        templatesService.sendVerificationEmail(user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                emailVerificationToken,
                dateHelper.getTimeLeftToExpiry(now, emailVerificationTokenExpiry, "hour"));

        return responseBuilder.buildUserResponse(
                null, HttpStatus.OK, "Verification email sent successfully.");
    }

    @Override
    @Transactional
    public UnravelDocsDataResponse<Void> verifyEmail(String email, String token) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User does not exist."));

        if (user.isVerified()) {
            throw new BadRequestException("User is already verified. Please login.");
        }

        UserVerification userVerification = user.getUserVerification();
        if (!userVerification.getEmailVerificationToken().equals(token)) {
            throw new BadRequestException("Invalid email verification token.");
        }

        if (userVerification.getEmailVerificationTokenExpiry().isBefore(OffsetDateTime.now())) {
            userVerification.setStatus(VerifiedStatus.EXPIRED);
            userRepository.save(user);
            throw new BadRequestException("Email verification token has expired.");
        }

        userVerification.setEmailVerificationToken(null);
        userVerification.setEmailVerified(true);
        userVerification.setEmailVerificationTokenExpiry(null);
        userVerification.setStatus(VerifiedStatus.VERIFIED);

        user.setVerified(userVerification.getStatus().equals(VerifiedStatus.VERIFIED));
        user.setActive(true);

        userRepository.save(user);

        return responseBuilder.buildUserResponse(
                null, HttpStatus.OK, "Email verified successfully");
    }
}
