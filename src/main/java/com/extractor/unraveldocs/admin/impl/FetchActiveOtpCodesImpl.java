package com.extractor.unraveldocs.admin.impl;

import com.extractor.unraveldocs.admin.dto.ActiveOtpData;
import com.extractor.unraveldocs.admin.dto.response.ActiveOtpListData;
import com.extractor.unraveldocs.admin.interfaces.FetchActiveOtpCodes;
import com.extractor.unraveldocs.admin.model.Otp;
import com.extractor.unraveldocs.admin.repository.OtpRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FetchActiveOtpCodesImpl implements FetchActiveOtpCodes {
    private final OtpRepository otpRepository;
    private final ResponseBuilderService responseBuilderService;

    @Override
    @Transactional(readOnly = true)
    public UnravelDocsResponse<ActiveOtpListData> fetchActiveOtpCodes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Otp> otpPage = otpRepository.findActiveOtps(pageable);

        List<ActiveOtpData> activeOtps = otpPage.getContent().stream().map(otp -> new ActiveOtpData(
                otp.getId(),
                otp.getOtpCode(),
                otp.getUser().getFirstName() + " " + otp.getUser().getLastName(),
                otp.isUsed(),
                otp.getUsedAt() != null ? otp.getUsedAt().toString() : null,
                otp.isExpired(),
                otp.getExpiresAt().toString(),
                otp.getCreatedAt().toString()
        )).toList();

        ActiveOtpListData data = new ActiveOtpListData(
                activeOtps,
                otpPage.getTotalElements(),
                otpPage.getTotalPages(),
                otpPage.getNumber(),
                otpPage.getSize()
        );

        return responseBuilderService.buildUserResponse(
                data,
                HttpStatus.OK,
                "Active OTP codes fetched successfully"
        );
    }
}
