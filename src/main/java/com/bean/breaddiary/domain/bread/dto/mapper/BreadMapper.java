package com.bean.breaddiary.domain.bread.dto.mapper;

import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteItemResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadCatalogItemResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadCatalogListResponse;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCatalogStats;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BreadMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stickerNumber", source = "stickerNumber")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "breadType", source = "request.breadType")
    @Mapping(target = "imageUrl", source = "imageUrl")
    @Mapping(target = "createdBy", source = "userId")
    Bread mapToBread(CreateNewBreadRecordRequest request, Integer stickerNumber, String imageUrl, UUID userId);

    @Mapping(target = "breadId", source = "bread.id")
    @Mapping(target = "name", source = "bread.name")
    @Mapping(target = "breadType", source = "bread.breadType")
    @Mapping(target = "stickerNumber", source = "bread.stickerNumber")
    @Mapping(target = "imageUrl", source = "bread.imageUrl")
    @Mapping(target = "eatCount", expression = "java(resolveEatCount(eatCount))")
    BreadAutocompleteItemResponse mapToAutocompleteItem(Bread bread, Long eatCount);

    default BreadAutocompleteResponse mapToAutocompleteResponse(List<Bread> breads, Map<UUID, Long> eatCounts) {
        List<BreadAutocompleteItemResponse> items = breads.stream()
                .map(bread -> mapToAutocompleteItem(
                        bread,
                        eatCounts.get(bread.getId())
                ))
                .toList();

        return new BreadAutocompleteResponse(items);
    }

    default Long resolveEatCount(Long eatCount) {
        return eatCount == null ? 0L : eatCount;
    }

    @Mapping(target = "breadId", source = "bread.id")
    @Mapping(target = "stickerNumber", source = "bread.stickerNumber")
    @Mapping(target = "name", source = "bread.name")
    @Mapping(target = "breadType", source = "bread.breadType")
    @Mapping(target = "imageUrl", source = "bread.imageUrl")
    @Mapping(target = "isCollected", expression = "java(isCollected(stats))")
    @Mapping(target = "eatCount", expression = "java(resolveEatCount(stats))")
    @Mapping(target = "avgRating", expression = "java(resolveAvgRating(stats))")
    @Mapping(target = "latestPhotoUrl", expression = "java(resolveLatestPhotoUrl(stats))")
    @Mapping(target = "latestEatenDate", expression = "java(stats == null ? null : stats.latestEatenDate())")
    BreadCatalogItemResponse mapToCatalogItem(Bread bread, BreadRecordCatalogStats stats);

    default BreadCatalogListResponse mapToCatalogListResponse(
            List<Bread> breads,
            Map<UUID, BreadRecordCatalogStats> statsMap,
            String nextCursor,
            boolean hasMore,
            long totalCount
    ) {
        List<BreadCatalogItemResponse> items = breads.stream()
                .map(bread -> mapToCatalogItem(
                        bread,
                        statsMap.get(bread.getId())
                ))
                .toList();

        return new BreadCatalogListResponse(
                items,
                nextCursor,
                hasMore,
                totalCount
        );
    }

    default Boolean isCollected(BreadRecordCatalogStats stats) {
        return stats != null && resolveEatCount(stats) > 0;
    }

    default Long resolveEatCount(BreadRecordCatalogStats stats) {
        return stats == null || stats.eatCount() == null ? 0L : stats.eatCount();
    }

    default Double resolveAvgRating(BreadRecordCatalogStats stats) {
        if (stats == null || stats.avgRating() == null) {
            return null;
        }

        return BigDecimal.valueOf(stats.avgRating())
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    default String resolveLatestPhotoUrl(BreadRecordCatalogStats stats) {
        if (stats == null || stats.latestPhotoUrl() == null) {
            return null;
        }

        return toThumbnailUrl(stats.latestPhotoUrl());
    }

    default String toThumbnailUrl(String photoUrl) {
        int extensionIndex = photoUrl.lastIndexOf('.');
        if (extensionIndex < 0) {
            return photoUrl + "_thumb";
        }

        return photoUrl.substring(0, extensionIndex)
                + "_thumb"
                + photoUrl.substring(extensionIndex);
    }
}
