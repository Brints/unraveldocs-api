package com.extractor.unraveldocs.auth.interfaces;

import com.extractor.unraveldocs.auth.dto.LoginData;
import com.extractor.unraveldocs.auth.dto.request.LoginRequestDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsDataResponse;

public interface LoginUserService {
    UnravelDocsDataResponse<LoginData> loginUser(LoginRequestDto request);
}
