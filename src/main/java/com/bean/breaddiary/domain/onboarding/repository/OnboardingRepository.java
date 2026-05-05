package com.bean.breaddiary.domain.onboarding.repository;

import com.bean.breaddiary.domain.onboarding.entity.OnboardingBread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OnboardingRepository extends JpaRepository<OnboardingBread, UUID> {

    @Query("""
            select ob.bread.id
            from OnboardingBread ob
            where ob.userId = :userId
              and ob.bread.id in :breadIds
            """)
    List<UUID> findSelectedBreadIdsByUserIdAndBreadIds(
            @Param("userId") UUID userId,
            @Param("breadIds") Collection<UUID> breadIds
    );

    @Modifying
    @Query("""
            delete
            from OnboardingBread ob
            where ob.userId = :userId
            """)
    void deleteAllByUserId(@Param("userId") UUID userId);
}
