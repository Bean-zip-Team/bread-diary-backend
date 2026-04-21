package com.bean.breaddiary.domain.bread.entity;

import com.bean.breaddiary.domain.breadtype.entity.BreadType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Builder
@Table(
        name = "breads",
        indexes = {
                @Index(name = "idx_breads_name", columnList = "name"),
                @Index(name = "idx_breads_sticker_number", columnList = "sticker_number", unique = true),
                @Index(name = "idx_breads_bread_type_id", columnList = "bread_type_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bread {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sticker_number", nullable = false, unique = true)
    private Integer stickerNumber;

    @Column(name = "name", length = 30, nullable = false, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bread_type_id", nullable = false)
    private BreadType breadType;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isUserCreated() {
        return createdBy != null;
    }

    public void clearCreator() {
        this.createdBy = null;
    }

    public boolean updateSystemCatalog(
            String name,
            BreadType breadType,
            String imageUrl
    ) {
        boolean changed = !Objects.equals(this.name, name)
                || !Objects.equals(this.breadType, breadType)
                || !Objects.equals(this.imageUrl, imageUrl);

        if (changed) {
            this.name = name;
            this.breadType = breadType;
            this.imageUrl = imageUrl;
        }

        return changed;
    }
}
