package com.bean.breaddiary.domain.bread.repository;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadtype.entity.BreadType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BreadRepository extends JpaRepository<Bread, UUID> {

    Optional<Bread> findByName(String name);

    Optional<Bread> findByStickerNumber(Integer stickerNumber);

    Optional<Bread> findTopByOrderByStickerNumberDesc();

    List<Bread> findAllByCreatedBy(UUID createdBy);

    List<Bread> findAllByCreatedByIsNullOrderByStickerNumberAsc();

    boolean existsByName(String name);

    List<Bread> findByNameContainingIgnoreCaseOrderByStickerNumberAsc(String name, Pageable pageable);

    @Query("""
            select b
            from Bread b
            where lower(b.name) like lower(concat('%', :name, '%'))
              and (:cursorStickerNumber is null or b.stickerNumber > :cursorStickerNumber)
              and (b.createdBy is null or (:userId is not null and b.createdBy = :userId))
            order by b.stickerNumber asc
            """)
    List<Bread> findAutocompleteByNameContainingAfterStickerNumber(
            @Param("name") String name,
            @Param("cursorStickerNumber") Integer cursorStickerNumber,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
            select b
            from Bread b
            left join BreadRecord br
                on br.bread = b
                and br.deletedAt is null
            group by b
            order by count(br) desc, b.stickerNumber asc
            """)
    List<Bread> findPopularOrderByRecordCountDesc(Pageable pageable);

    @Query("""
            select b
            from Bread b
            left join BreadRecord br
                on br.bread = b
                and br.deletedAt is null
            where (b.createdBy is null or (:userId is not null and b.createdBy = :userId))
            group by b
            having (:recordCountCursor is null
                    or count(br) < :recordCountCursor
                    or (count(br) = :recordCountCursor and b.stickerNumber > :stickerNumberCursor))
            order by count(br) desc, b.stickerNumber asc
            """)
    List<Bread> findPopularAutocompleteAfterCursor(
            @Param("recordCountCursor") Long recordCountCursor,
            @Param("stickerNumberCursor") Integer stickerNumberCursor,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
            select count(br)
            from BreadRecord br
            where br.bread.id = :breadId
              and br.deletedAt is null
            """)
    Long countActiveRecordsByBreadId(@Param("breadId") UUID breadId);

    @Query("""
            select b
            from Bread b
            where (:search is null or lower(b.name) like lower(concat('%', :search, '%')))
              and (:breadType is null or b.breadType = :breadType)
              and (b.createdBy is null or (:userId is not null and b.createdBy = :userId))
            order by b.stickerNumber asc
            """)
    List<Bread> findCatalogCandidates(
            @Param("search") String search,
            @Param("breadType") BreadType breadType,
            @Param("userId") UUID userId
    );
}
