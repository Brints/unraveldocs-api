package com.extractor.unraveldocs.auth.interfaces;

import com.extractor.unraveldocs.auth.dto.SignupData;
import com.extractor.unraveldocs.auth.dto.request.SignUpRequestDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;

public interface SignupUserService {
   UnravelDocsDataResponse<SignupData> registerUser(SignUpRequestDto request);
}
