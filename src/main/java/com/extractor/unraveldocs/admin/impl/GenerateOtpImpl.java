package com.extractor.unraveldocs.admin.impl;

import com.extractor.unraveldocs.admin.dto.request.OtpRequestDto;
import com.extractor.unraveldocs.admin.interfaces.GenerateOtpService;
import com.extractor.unraveldocs.admin.model.Otp;
import com.extractor.unraveldocs.admin.repository.OtpRepository;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import com.extractor.unraveldocs.utils.userlib.DateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerateOtpImpl implements GenerateOtpService {
    private final GenerateVerificationToken generateVerificationToken;
    private final OtpRepository otpRepository;
    private final DateHelper dateHelper;
    private final ResponseBuilderService responseBuilder;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UnravelDocsResponse<List<String>> generateOtp(OtpRequestDto request) {
        var len = request.getLength() > 6 ? request.getLength() : 6;
        var count = request.getCount() > 1 ? request.getCount() : 1;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assert authentication != null;
        String userEmail = authentication.getName();

        User superAdminUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("Super admin user not found"));

        OffsetDateTime now = OffsetDateTime.now();
        var expiry = dateHelper.setExpiryDate(now, "minute", 10);

        List<String> otpCodes = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String otp = generateVerificationToken.generateOtp(len);

            var otpEntity = new Otp();
            otpEntity.setOtpCode(otp);
            otpEntity.setExpiresAt(expiry);
            otpEntity.setUser(superAdminUser);
            otpEntity.setExpired(false);
            otpEntity.setUsed(false);
            otpEntity.setExpired(false);
            otpRepository.save(otpEntity);

            otpCodes.add(otp);
        }

        String message = count > 1
                ? count + " OTPs generated successfully"
                : "OTP generated successfully";

        return responseBuilder.buildUserResponse(
                otpCodes,
                HttpStatus.OK,
                message
        );
    }
}
