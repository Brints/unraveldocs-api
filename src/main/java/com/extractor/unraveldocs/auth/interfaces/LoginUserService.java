package com.extractor.unraveldocs.auth.interfaces;

import com.extractor.unraveldocs.auth.dto.LoginResult;
import com.extractor.unraveldocs.auth.dto.request.LoginRequestDto;

public interface LoginUserService {
    LoginResult loginUser(LoginRequestDto request);
}
