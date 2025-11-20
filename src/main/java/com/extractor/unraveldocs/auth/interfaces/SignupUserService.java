package com.extractor.unraveldocs.auth.interfaces;

import com.extractor.unraveldocs.auth.dto.SignupData;
import com.extractor.unraveldocs.auth.dto.request.SignupRequestDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface SignupUserService {
   UnravelDocsResponse<SignupData> registerUser(SignupRequestDto request);
}
