package com.bean.breaddiary.domain.breadrecord.dto.projection;

import java.time.LocalDate;
import java.util.UUID;

public record BreadRecordCatalogStats(
        UUID breadId,
        Long eatCount,
        Double avgRating,
        String latestPhotoUrl,
        LocalDate latestEatenDate
) {
}
