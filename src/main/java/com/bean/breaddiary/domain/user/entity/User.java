package com.bean.breaddiary.domain.user.entity;

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
        name = "users",
        indexes = {
                @Index(name = "idx_users_toss_user_key", columnList = "toss_user_key", unique = true),
                @Index(name = "idx_users_email", columnList = "email", unique = true),
                @Index(name = "idx_users_nickname", columnList = "nickname"),
                @Index(name = "idx_users_deleted_at", columnList = "deleted_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nickname", length = 30, nullable = false)
    private String nickname;

    @Column(name = "toss_user_key", length = 100, unique = true)
    private String tossUserKey;

    @Column(name = "email", length = 255, unique = true)
    private String email;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void updateFromToss(
            String tossUserKey,
            String nickname,
            String email
    ) {
        this.tossUserKey = tossUserKey;
        this.nickname = nickname;
        this.email = email;
    }

    public void activate(
            String tossUserKey,
            String nickname,
            String email
    ) {
        updateFromToss(tossUserKey, nickname, email);
        this.deletedAt = null;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
