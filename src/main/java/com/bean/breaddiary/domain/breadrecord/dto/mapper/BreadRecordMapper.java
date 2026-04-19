package com.bean.breaddiary.domain.breadrecord.dto.mapper;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileRecordResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileStatsResponse;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCatalogStats;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCatalogStatsProjection;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordLatestPhotoProjection;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordDetailResponse;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordCreateResponse;
import com.bean.breaddiary.domain.breadrecord.entity.BreadRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BreadRecordMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "bread", source = "bread")
    @Mapping(target = "photoUrl", source = "photoUrl")
    @Mapping(target = "eatenDate", source = "eatenDate")
    BreadRecord mapToBreadRecord(
            CreateBreadRecordRequest request,
            UUID userId,
            Bread bread,
            String photoUrl,
            LocalDate eatenDate
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "bread", source = "bread")
    @Mapping(target = "photoUrl", source = "photoUrl")
    @Mapping(target = "eatenDate", source = "eatenDate")
    BreadRecord mapToBreadRecord(
            CreateNewBreadRecordRequest request,
            UUID userId,
            Bread bread,
            String photoUrl,
            LocalDate eatenDate
    );

    @Mapping(target = "breadId", source = "breadRecord.bread.id")
    @Mapping(target = "stickerNumber", source = "breadRecord.bread.stickerNumber")
    @Mapping(target = "name", source = "breadRecord.bread.name")
    @Mapping(target = "breadType", source = "breadRecord.bread.breadType")
    @Mapping(target = "imageUrl", source = "breadRecord.bread.imageUrl")
    @Mapping(target = "photoThumbnailUrl", expression = "java(com.bean.breaddiary.global.common.ThumbnailUrlUtils.toThumbnailUrl(breadRecord.getPhotoUrl()))")
    @Mapping(target = "isFirstRecord", source = "isFirstRecord")
    BreadRecordCreateResponse mapToCreateResponse(
            BreadRecord breadRecord,
            Boolean isFirstRecord
    );

    @Mapping(target = "breadId", source = "breadRecord.bread.id")
    @Mapping(target = "stickerNumber", source = "breadRecord.bread.stickerNumber")
    @Mapping(target = "name", source = "breadRecord.bread.name")
    @Mapping(target = "breadType", source = "breadRecord.bread.breadType")
    @Mapping(target = "breadTypeLabel", expression = "java(toBreadTypeLabel(breadRecord.getBread().getBreadType()))")
    @Mapping(target = "imageUrl", source = "breadRecord.bread.imageUrl")
    @Mapping(target = "photoThumbnailUrl", expression = "java(com.bean.breaddiary.global.common.ThumbnailUrlUtils.toThumbnailUrl(breadRecord.getPhotoUrl()))")
    BreadRecordDetailResponse mapToDetailResponse(BreadRecord breadRecord);

    default Map<UUID, BreadRecordCatalogStats> mapToCatalogStatsMap(
            List<BreadRecordCatalogStatsProjection> statsProjections,
            List<BreadRecordLatestPhotoProjection> latestPhotoProjections
    ) {
        Map<UUID, String> latestPhotoUrls = latestPhotoProjections.stream()
                .collect(Collectors.toMap(
                        BreadRecordLatestPhotoProjection::getBreadId,
                        BreadRecordLatestPhotoProjection::getLatestPhotoUrl,
                        (left, right) -> left
                ));

        return statsProjections.stream()
                .map(stats -> new BreadRecordCatalogStats(
                        stats.getBreadId(),
                        stats.getEatCount(),
                        stats.getAvgRating(),
                        latestPhotoUrls.get(stats.getBreadId()),
                        stats.getLatestEatenDate()
                ))
                .collect(Collectors.toMap(
                        BreadRecordCatalogStats::getBreadId,
                        stats -> stats
                ));
    }

    @Mapping(target = "photoThumbnailUrl", expression = "java(com.bean.breaddiary.global.common.ThumbnailUrlUtils.toThumbnailUrl(breadRecord.getPhotoUrl()))")
    BreadProfileRecordResponse mapToProfileRecord(BreadRecord breadRecord);

    default List<BreadProfileRecordResponse> mapToProfileRecords(List<BreadRecord> breadRecords) {
        return breadRecords.stream()
                .map(this::mapToProfileRecord)
                .toList();
    }

    default BreadProfileStatsResponse mapToProfileStats(List<BreadRecord> breadRecords) {
        if (breadRecords.isEmpty()) {
            return new BreadProfileStatsResponse(
                    0L,
                    null,
                    null
            );
        }

        double avgRating = breadRecords.stream()
                .mapToInt(BreadRecord::getRating)
                .average()
                .orElse(0);
        LocalDateTime firstRecordedAt = breadRecords.stream()
                .map(BreadRecord::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        return new BreadProfileStatsResponse(
                (long) breadRecords.size(),
                roundRating(avgRating),
                firstRecordedAt
        );
    }

    default Double roundRating(Double rating) {
        if (rating == null) {
            return null;
        }

        return BigDecimal.valueOf(rating)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    default String toBreadTypeLabel(com.bean.breaddiary.domain.bread.entity.BreadType breadType) {
        return breadType == null ? null : breadType.getLabel();
    }
}
