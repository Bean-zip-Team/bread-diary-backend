package com.bean.breaddiary.domain.auth.service;

import com.bean.breaddiary.domain.auth.dto.request.TossLoginRequest;
import com.bean.breaddiary.domain.auth.dto.response.AuthTokenResponse;
import com.bean.breaddiary.domain.onboarding.service.OnboardingComplexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthComplexService {

    private final AuthService authService;
    private final OnboardingComplexService onboardingComplexService;

    @Transactional
    public AuthTokenResponse loginWithToss(TossLoginRequest request) {
        AuthTokenResponse response = authService.loginWithToss(request);
        onboardingComplexService.synchronizeSelectedBreads(response.getUserId(), request == null ? null : request.getSelectedBreadIds());
        return response;
    }
}
