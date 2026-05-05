package com.bean.breaddiary.domain.onboarding.service;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.onboarding.entity.OnboardingBread;
import com.bean.breaddiary.domain.onboarding.repository.OnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingService {

    private final OnboardingRepository onboardingRepository;

    public Set<UUID> findSelectedBreadIds(UUID userId, Collection<UUID> breadIds) {
        if (userId == null || breadIds == null || breadIds.isEmpty()) {
            return Set.of();
        }

        return new LinkedHashSet<>(onboardingRepository.findSelectedBreadIdsByUserIdAndBreadIds(userId, breadIds));
    }

    public boolean isSelected(UUID userId, UUID breadId) {
        return !findSelectedBreadIds(userId, List.of(breadId)).isEmpty();
    }

    @Transactional
    public void replaceSelectedBreads(UUID userId, List<Bread> breads) {
        onboardingRepository.deleteAllByUserId(userId);

        if (breads == null || breads.isEmpty()) {
            return;
        }

        List<OnboardingBread> onboardingBreads = breads.stream()
                .map(bread -> OnboardingBread.builder()
                        .userId(userId)
                        .bread(bread)
                        .build())
                .toList();

        onboardingRepository.saveAll(onboardingBreads);
    }

    @Transactional
    public void deleteAllByUserId(UUID userId) {
        onboardingRepository.deleteAllByUserId(userId);
    }
}
