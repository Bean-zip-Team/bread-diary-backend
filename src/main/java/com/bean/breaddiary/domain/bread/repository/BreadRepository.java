package com.bean.breaddiary.domain.bread.repository;

import com.bean.breaddiary.domain.bread.entity.Bread;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BreadRepository extends JpaRepository<Bread, UUID> {

    Optional<Bread> findByName(String name);

    Optional<Bread> findByStickerNumber(Integer stickerNumber);

    Optional<Bread> findTopByOrderByStickerNumberDesc();

    boolean existsByName(String name);

    List<Bread> findByNameContainingIgnoreCaseOrderByStickerNumberAsc(String name, Pageable pageable);

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
}
