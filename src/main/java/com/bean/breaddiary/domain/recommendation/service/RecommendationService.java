package com.bean.breaddiary.domain.recommendation.service;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.recommendation.entity.DailyRecommendation;
import com.bean.breaddiary.domain.recommendation.repository.DailyRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final DailyRecommendationRepository dailyRecommendationRepository;

    public List<DailyRecommendation> findRecommendationsByDate(LocalDate recommendationDate) {
        return dailyRecommendationRepository.findAllByRecommendationDateOrderByDisplayOrderAsc(recommendationDate);
    }

    public List<UUID> findRecentlyRecommendedBreadIds(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return List.of();
        }

        return dailyRecommendationRepository.findDistinctBreadIdsByRecommendationDateBetween(startDate, endDate);
    }

    @Transactional
    public List<DailyRecommendation> saveRecommendations(LocalDate recommendationDate, List<Bread> breads) {
        List<DailyRecommendation> recommendations = IntStream.range(0, breads.size())
                .mapToObj(index -> DailyRecommendation.builder()
                        .recommendationDate(recommendationDate)
                        .bread(breads.get(index))
                        .displayOrder(index + 1)
                        .build())
                .toList();

        return dailyRecommendationRepository.saveAll(recommendations).stream()
                .sorted((left, right) -> Integer.compare(left.getDisplayOrder(), right.getDisplayOrder()))
                .toList();
    }
}
