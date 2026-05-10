package com.bean.breaddiary.domain.onboarding.service;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.service.BreadService;
import com.bean.breaddiary.domain.onboarding.entity.OnboardingBreadCatalog;
import com.bean.breaddiary.domain.onboarding.dto.mapper.OnboardingMapper;
import com.bean.breaddiary.domain.onboarding.dto.response.OnboardingBreadItemResponse;
import com.bean.breaddiary.domain.onboarding.dto.response.OnboardingBreadListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingComplexService {

    private final OnboardingService onboardingService;
    private final BreadService breadService;
    private final OnboardingMapper onboardingMapper;

    public OnboardingBreadListResponse getOnboardingBreads() {
        Map<String, Bread> breadsByCatalogName = findOnboardingBreadsByCatalogName();

        List<OnboardingBreadItemResponse> items = Arrays.stream(OnboardingBreadCatalog.values())
                .map(catalog -> onboardingMapper.mapToItem(catalog, requireBread(breadsByCatalogName, catalog)))
                .toList();

        return onboardingMapper.mapToListResponse(items);
    }

    @Transactional
    public void synchronizeSelectedBreads(UUID userId, List<UUID> selectedBreadIds) {
        if (selectedBreadIds == null) {
            return;
        }

        Set<UUID> deduplicatedIds = new LinkedHashSet<>(selectedBreadIds);
        if (deduplicatedIds.contains(null)) {
            throw invalidOnboardingSelection();
        }

        if (deduplicatedIds.isEmpty()) {
            onboardingService.replaceSelectedBreads(userId, List.of());
            return;
        }

        Map<UUID, Bread> allowedBreadsById = findAllowedOnboardingBreadsById();
        List<Bread> selectedBreads = deduplicatedIds.stream()
                .map(allowedBreadsById::get)
                .toList();

        if (selectedBreads.stream().anyMatch(java.util.Objects::isNull)) {
            throw invalidOnboardingSelection();
        }

        onboardingService.replaceSelectedBreads(userId, selectedBreads);
    }

    private Map<UUID, Bread> findAllowedOnboardingBreadsById() {
        return findOnboardingBreadsByCatalogName().values().stream()
                .collect(LinkedHashMap::new, (map, bread) -> map.put(bread.getId(), bread), Map::putAll);
    }

    private Map<String, Bread> findOnboardingBreadsByCatalogName() {
        List<String> catalogNames = OnboardingBreadCatalog.catalogNames();
        List<Bread> breads = breadService.findAllSystemCatalogBreadsByNames(catalogNames);

        if (breads.size() != catalogNames.size()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "온보딩 빵 목록 구성이 올바르지 않습니다."
            );
        }

        Map<String, Bread> breadsByCatalogName = new LinkedHashMap<>();
        for (Bread bread : breads) {
            breadsByCatalogName.put(bread.getName(), bread);
        }

        for (OnboardingBreadCatalog catalog : OnboardingBreadCatalog.values()) {
            if (!breadsByCatalogName.containsKey(catalog.getCatalogName())) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "온보딩 빵 목록 구성이 올바르지 않습니다."
                );
            }
        }

        return breadsByCatalogName;
    }

    private Bread requireBread(Map<String, Bread> breadsByCatalogName, OnboardingBreadCatalog catalog) {
        Bread bread = breadsByCatalogName.get(catalog.getCatalogName());
        if (bread == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "온보딩 빵 목록 구성이 올바르지 않습니다."
            );
        }
        return bread;
    }

    private ResponseStatusException invalidOnboardingSelection() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "온보딩에서 선택한 빵 정보가 올바르지 않습니다."
        );
    }
}
