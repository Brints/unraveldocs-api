package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.request.OtpRequestDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

import java.util.List;

public interface GenerateOtpService {
    UnravelDocsResponse<List<String>> generateOtp(OtpRequestDto request);
}
