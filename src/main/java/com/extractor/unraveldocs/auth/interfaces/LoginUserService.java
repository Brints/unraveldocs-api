package com.extractor.unraveldocs.auth.interfaces;

import com.extractor.unraveldocs.auth.dto.LoginData;
import com.extractor.unraveldocs.auth.dto.request.LoginRequestDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface LoginUserService {
    UnravelDocsResponse<LoginData> loginUser(LoginRequestDto request);
}
