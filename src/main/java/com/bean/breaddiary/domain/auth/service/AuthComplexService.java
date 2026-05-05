package com.bean.breaddiary.domain.auth.service;

import com.bean.breaddiary.domain.auth.dto.request.TossLoginRequest;
import com.bean.breaddiary.domain.auth.dto.response.AuthTokenResponse;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.service.BreadService;
import com.bean.breaddiary.domain.onboarding.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthComplexService {

    private final AuthService authService;
    private final BreadService breadService;
    private final OnboardingService onboardingService;

    @Transactional
    public AuthTokenResponse loginWithToss(TossLoginRequest request) {
        AuthTokenResponse response = authService.loginWithToss(request);
        synchronizeOnboardingSelections(response.getUserId(), request);
        return response;
    }

    private void synchronizeOnboardingSelections(UUID userId, TossLoginRequest request) {
        if (request == null || request.getSelectedBreadIds() == null) {
            return;
        }

        Set<UUID> deduplicatedIds = new LinkedHashSet<>(request.getSelectedBreadIds());
        if (deduplicatedIds.contains(null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "온보딩에서 선택한 빵 정보가 올바르지 않습니다."
            );
        }
        if (deduplicatedIds.isEmpty()) {
            onboardingService.replaceSelectedBreads(userId, List.of());
            return;
        }

        List<Bread> breads = breadService.findAllSystemCatalogBreadsByIds(List.copyOf(deduplicatedIds));
        if (breads.size() != deduplicatedIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "온보딩에서 선택한 빵 정보가 올바르지 않습니다."
            );
        }

        onboardingService.replaceSelectedBreads(userId, breads);
    }
}
