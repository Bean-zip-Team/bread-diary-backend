package com.bean.breaddiary.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@Table(
        name = "user_sessions",
        indexes = {
                @Index(name = "idx_user_sessions_user_id_revoked_at", columnList = "user_id,revoked_at"),
                @Index(name = "idx_user_sessions_current_jti", columnList = "current_jti", unique = true),
                @Index(name = "idx_user_sessions_refresh_expires_at", columnList = "refresh_expires_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSession {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "refresh_token_hash", length = 255, nullable = false)
    private String refreshTokenHash;

    @Column(name = "current_jti", length = 100, nullable = false, unique = true)
    private String currentJti;

    @Column(name = "refresh_expires_at", nullable = false)
    private LocalDateTime refreshExpiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isRefreshExpiredAt(LocalDateTime now) {
        return !refreshExpiresAt.isAfter(now);
    }

    public boolean isActiveAt(LocalDateTime now) {
        return !isRevoked() && !isRefreshExpiredAt(now);
    }

    public boolean matchesRefreshTokenHash(String refreshTokenHash) {
        return this.refreshTokenHash != null && this.refreshTokenHash.equals(refreshTokenHash);
    }

    public boolean matchesCurrentJti(String currentJti) {
        return this.currentJti != null && this.currentJti.equals(currentJti);
    }

    public void rotateRefreshToken(
            String refreshTokenHash,
            String currentJti,
            LocalDateTime refreshExpiresAt
    ) {
        this.refreshTokenHash = refreshTokenHash;
        this.currentJti = currentJti;
        this.refreshExpiresAt = refreshExpiresAt;
    }

    public void revoke(LocalDateTime revokedAt) {
        if (this.revokedAt == null) {
            this.revokedAt = revokedAt;
        }
    }
}
