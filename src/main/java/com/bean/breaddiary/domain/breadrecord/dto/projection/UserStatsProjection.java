package com.bean.breaddiary.domain.breadrecord.dto.projection;

public interface UserStatsProjection {

    Long getTotalRecords();

    Long getTotalStickers();

    Long getUniqueShops();

    Double getAvgRating();
}
