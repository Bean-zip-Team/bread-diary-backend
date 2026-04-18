package com.bean.breaddiary.domain.breadrecord.repository;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadrecord.entity.BreadRecord;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCatalogStatsProjection;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCountProjection;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordLatestPhotoProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BreadRecordRepository extends JpaRepository<BreadRecord, UUID> {

    List<BreadRecord> findAllByUserId(UUID userId);
    List<BreadRecord> findAllByUserIdAndBread(UUID userId, Bread bread);
    List<BreadRecord> findAllByUserIdAndBreadAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, Bread bread);
    long countByUserIdAndBread(UUID userId, Bread bread);
    boolean existsByUserIdAndBreadAndDeletedAtIsNull(UUID userId, Bread bread);

    @Query("""
            select br.bread.id as breadId, count(br) as eatCount
            from BreadRecord br
            where br.userId = :userId
              and br.bread.id in :breadIds
              and br.deletedAt is null
            group by br.bread.id
            """)
    List<BreadRecordCountProjection> countActiveRecordsByBreadIds(
            @Param("userId") UUID userId,
            @Param("breadIds") List<UUID> breadIds
    );

    @Query("""
            select br.bread.id as breadId,
                   count(br) as eatCount,
                   avg(br.rating) as avgRating,
                   max(br.eatenDate) as latestEatenDate
            from BreadRecord br
            where br.userId = :userId
              and br.bread.id in :breadIds
              and br.deletedAt is null
            group by br.bread.id
            """)
    List<BreadRecordCatalogStatsProjection> findCatalogStatsByBreadIds(
            @Param("userId") UUID userId,
            @Param("breadIds") List<UUID> breadIds
    );

    @Query("""
            select br.bread.id as breadId,
                   br.photoUrl as latestPhotoUrl
            from BreadRecord br
            where br.userId = :userId
              and br.bread.id in :breadIds
              and br.deletedAt is null
              and br.createdAt = (
                  select max(latest.createdAt)
                  from BreadRecord latest
                  where latest.userId = :userId
                    and latest.bread.id = br.bread.id
                    and latest.deletedAt is null
              )
            """)
    List<BreadRecordLatestPhotoProjection> findLatestPhotoUrlsByBreadIds(
            @Param("userId") UUID userId,
            @Param("breadIds") List<UUID> breadIds
    );
}
