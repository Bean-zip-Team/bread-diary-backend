package com.bean.breaddiary.domain.breadrecord.dto.projection;

import java.time.LocalDate;
import java.util.UUID;

public interface BreadRecordCatalogStatsProjection {

    UUID getBreadId();

    Long getEatCount();

    Double getAvgRating();

    LocalDate getLatestEatenDate();
}
