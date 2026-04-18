package com.bean.breaddiary.domain.user.dto.mapper;

import com.bean.breaddiary.domain.breadrecord.dto.projection.UserStatsProjection;
import com.bean.breaddiary.domain.user.dto.response.UserMeResponse;
import com.bean.breaddiary.domain.user.dto.response.UserStatsResponse;
import com.bean.breaddiary.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "stats", source = "stats")
    UserMeResponse mapToUserMeResponse(User user, UserStatsResponse stats);

    default UserStatsResponse mapToUserStatsResponse(UserStatsProjection projection) {
        if (projection == null) {
            return new UserStatsResponse(
                    0L,
                    0L,
                    0.0
            );
        }

        return new UserStatsResponse(
                resolveCount(projection.getTotalRecords()),
                resolveCount(projection.getUniqueShops()),
                roundRating(projection.getAvgRating())
        );
    }

    default Long resolveCount(Long count) {
        return count == null ? 0L : count;
    }

    default Double roundRating(Double rating) {
        if (rating == null) {
            return 0.0;
        }

        return BigDecimal.valueOf(rating)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
