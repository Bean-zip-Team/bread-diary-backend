package com.bean.breaddiary.domain.breadrecord.dto.projection;

import java.util.UUID;

public interface BreadRecordCountProjection {

    UUID getBreadId();

    Long getEatCount();
}
