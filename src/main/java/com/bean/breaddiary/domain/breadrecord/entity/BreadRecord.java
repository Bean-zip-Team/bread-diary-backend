package com.bean.breaddiary.domain.breadrecord.entity;

import com.bean.breaddiary.domain.bread.entity.Bread;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@Table(
        name = "bread_records",
        indexes = {
                @Index(name = "idx_bread_records_user_id", columnList = "user_id"),
                @Index(name = "idx_bread_records_user_bread", columnList = "user_id,bread_id"),
                @Index(name = "idx_bread_records_created_at", columnList = "created_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BreadRecord {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bread_id", nullable = false)
    private Bread bread;

    @Column(name = "photo_url", length = 500, nullable = false)
    private String photoUrl;

    @Column(name = "shop_name", length = 50)
    private String shopName;

    @Builder.Default
    @Column(name = "eaten_date", nullable = false)
    private LocalDate eatenDate = LocalDate.now();

    @Min(1)
    @Max(5)
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Size(max = 500)
    @Column(name = "review", columnDefinition = "TEXT")
    private String review;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void update(
            String photoUrl,
            String shopName,
            LocalDate eatenDate,
            Integer rating,
            String review
    ) {
        this.photoUrl = photoUrl;
        this.shopName = shopName;
        this.eatenDate = eatenDate;
        this.rating = rating;
        this.review = review;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
