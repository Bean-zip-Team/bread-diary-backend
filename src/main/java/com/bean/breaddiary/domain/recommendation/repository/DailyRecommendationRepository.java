package com.bean.breaddiary.domain.recommendation.repository;

import com.bean.breaddiary.domain.recommendation.entity.DailyRecommendation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyRecommendationRepository extends JpaRepository<DailyRecommendation, UUID> {

    @EntityGraph(attributePaths = {"bread", "bread.breadType"})
    List<DailyRecommendation> findAllByRecommendationDateOrderByDisplayOrderAsc(LocalDate recommendationDate);

    @Query("""
            select distinct recommendation.bread.id
            from DailyRecommendation recommendation
            where recommendation.recommendationDate between :startDate and :endDate
            """)
    List<UUID> findDistinctBreadIdsByRecommendationDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
