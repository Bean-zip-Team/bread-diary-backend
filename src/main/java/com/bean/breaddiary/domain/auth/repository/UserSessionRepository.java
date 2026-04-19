package com.bean.breaddiary.domain.auth.repository;

import com.bean.breaddiary.domain.auth.entity.UserSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByIdAndRevokedAtIsNull(UUID id);

    List<UserSession> findAllByUserIdAndRevokedAtIsNull(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserSession userSession where userSession.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select userSession
            from UserSession userSession
            where userSession.id = :sessionId
              and userSession.revokedAt is null
            """)
    Optional<UserSession> findByIdAndRevokedAtIsNullForUpdate(@Param("sessionId") UUID sessionId);
}
