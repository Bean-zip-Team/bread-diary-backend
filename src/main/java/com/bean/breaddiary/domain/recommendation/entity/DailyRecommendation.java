package com.bean.breaddiary.domain.recommendation.entity;

import com.bean.breaddiary.domain.bread.entity.Bread;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@Table(
        name = "daily_bread_recommendations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_bread_recommendations_date_order",
                        columnNames = {"recommendation_date", "display_order"}
                ),
                @UniqueConstraint(
                        name = "uk_daily_bread_recommendations_date_bread",
                        columnNames = {"recommendation_date", "bread_id"}
                )
        },
        indexes = {
                @Index(name = "idx_daily_bread_recommendations_date", columnList = "recommendation_date"),
                @Index(name = "idx_daily_bread_recommendations_bread", columnList = "bread_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyRecommendation {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "recommendation_date", nullable = false)
    private LocalDate recommendationDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bread_id", nullable = false)
    private Bread bread;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
