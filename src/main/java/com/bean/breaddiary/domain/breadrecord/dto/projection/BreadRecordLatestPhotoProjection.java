package com.bean.breaddiary.domain.breadrecord.dto.projection;

import java.util.UUID;

public interface BreadRecordLatestPhotoProjection {

    UUID getBreadId();

    String getLatestPhotoUrl();
}
