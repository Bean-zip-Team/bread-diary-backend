package com.bean.breaddiary.domain.breadrecord.repository;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadrecord.entity.BreadRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BreadRecordRepository extends JpaRepository<BreadRecord, UUID> {

    List<BreadRecord> findAllByUserId(UUID userId);
    List<BreadRecord> findAllByUserIdAndBread(UUID userId, Bread bread);
    long countByUserIdAndBread(UUID userId, Bread bread);
    boolean existsByUserIdAndBreadAndDeletedAtIsNull(UUID userId, Bread bread);
}
