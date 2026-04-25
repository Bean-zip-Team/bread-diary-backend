package com.bean.breaddiary.domain.recommendation.dto.mapper;

import com.bean.breaddiary.domain.recommendation.dto.response.BreadTodayItemResponse;
import com.bean.breaddiary.domain.recommendation.dto.response.BreadTodayResponse;
import com.bean.breaddiary.domain.recommendation.entity.DailyRecommendation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RecommendationMapper {

    @Mapping(target = "id", source = "recommendation.bread.id")
    @Mapping(target = "name", source = "recommendation.bread.name")
    @Mapping(target = "type", source = "recommendation.bread.breadType.code")
    @Mapping(target = "imageUrl", expression = "java(resolveImageUrl(recommendation.getBread().getImageUrl(), userEatCount))")
    @Mapping(target = "totalRecordCount", expression = "java(resolveCount(totalRecordCount))")
    @Mapping(target = "isCollected", expression = "java(isCollected(userEatCount))")
    BreadTodayItemResponse mapToTodayItem(
            DailyRecommendation recommendation,
            Long totalRecordCount,
            Long userEatCount
    );

    default BreadTodayResponse mapToTodayResponse(
            LocalDate date,
            List<DailyRecommendation> recommendations,
            Map<UUID, Long> totalRecordCounts,
            Map<UUID, Long> userEatCounts
    ) {
        List<BreadTodayItemResponse> breads = recommendations.stream()
                .map(recommendation -> {
                    UUID breadId = recommendation.getBread().getId();
                    return mapToTodayItem(
                            recommendation,
                            totalRecordCounts.get(breadId),
                            userEatCounts.get(breadId)
                    );
                })
                .toList();

        return new BreadTodayResponse(date, breads);
    }

    default boolean isCollected(Long eatCount) {
        return resolveCount(eatCount) > 0;
    }

    default Long resolveCount(Long count) {
        return count == null ? 0L : count;
    }

    default String resolveImageUrl(String imageUrl, Long userEatCount) {
        if (isCollected(userEatCount)) {
            return imageUrl;
        }

        return toPlaceholderUrl(imageUrl);
    }

    private String toPlaceholderUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        int extensionIndex = imageUrl.lastIndexOf('.');
        if (extensionIndex < 0) {
            return imageUrl + "_placeholder";
        }

        return imageUrl.substring(0, extensionIndex)
                + "_placeholder"
                + imageUrl.substring(extensionIndex);
    }
}
