package com.bean.breaddiary.domain.breadrecord.repository;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.entity.BreadType;
import com.bean.breaddiary.domain.breadrecord.dto.projection.UserStatsProjection;
import com.bean.breaddiary.domain.breadrecord.entity.BreadRecord;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class BreadRecordRepositoryTest {

    @Autowired
    private BreadRecordRepository breadRecordRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findUserStatsByUserIdExcludesNullAndBlankShopNames() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Bread bread = saveBread();

        saveBreadRecord(userId, bread, "shop-a", 5, null);
        saveBreadRecord(userId, bread, "   ", 4, null);
        saveBreadRecord(userId, bread, "", 3, null);
        saveBreadRecord(userId, bread, null, 2, null);
        saveBreadRecord(userId, bread, "shop-b", 1, null);
        saveBreadRecord(userId, bread, "shop-a", 5, null);

        UserStatsProjection stats = breadRecordRepository.findUserStatsByUserId(userId);

        assertNotNull(stats);
        assertEquals(6L, stats.getTotalRecords());
        assertEquals(1L, stats.getTotalStickers());
        assertEquals(2L, stats.getUniqueShops());
        assertEquals(3.3333333333333335, stats.getAvgRating());
    }

    @Test
    void findUserStatsByUserIdIgnoresSoftDeletedRecords() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Bread bread = saveBread();

        saveBreadRecord(userId, bread, "shop-live", 5, null);
        saveBreadRecord(userId, bread, "shop-deleted", 1, LocalDateTime.of(2026, 4, 18, 12, 0));

        UserStatsProjection stats = breadRecordRepository.findUserStatsByUserId(userId);

        assertNotNull(stats);
        assertEquals(1L, stats.getTotalRecords());
        assertEquals(1L, stats.getTotalStickers());
        assertEquals(1L, stats.getUniqueShops());
        assertEquals(5.0, stats.getAvgRating());
    }

    @Test
    void findUserStatsByUserIdCountsDistinctActiveBreadsAsTotalStickers() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        Bread firstBread = saveBread(1);
        Bread secondBread = saveBread(2);
        Bread thirdBread = saveBread(3);
        Bread deletedOnlyBread = saveBread(4);

        saveBreadRecord(userId, firstBread, "shop-a", 5, null);
        saveBreadRecord(userId, firstBread, "shop-a", 4, null);
        saveBreadRecord(userId, secondBread, "shop-b", 3, null);
        saveBreadRecord(userId, thirdBread, "shop-c", 2, null);
        saveBreadRecord(userId, deletedOnlyBread, "shop-d", 1, LocalDateTime.of(2026, 4, 18, 12, 0));

        UserStatsProjection stats = breadRecordRepository.findUserStatsByUserId(userId);

        assertNotNull(stats);
        assertEquals(4L, stats.getTotalRecords());
        assertEquals(3L, stats.getTotalStickers());
        assertEquals(3L, stats.getUniqueShops());
        assertEquals(3.5, stats.getAvgRating());
    }

    private Bread saveBread() {
        return saveBread(1);
    }

    private Bread saveBread(int stickerNumber) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Bread bread = Bread.builder()
                .stickerNumber(stickerNumber)
                .name("bread-" + suffix)
                .breadType(BreadType.BREAD)
                .imageUrl("https://cdn.bread-diary.app/breads/test.webp")
                .build();

        entityManager.persist(bread);
        return bread;
    }

    private void saveBreadRecord(
            UUID userId,
            Bread bread,
            String shopName,
            int rating,
            LocalDateTime deletedAt
    ) {
        BreadRecord breadRecord = BreadRecord.builder()
                .userId(userId)
                .bread(bread)
                .photoUrl("https://cdn.bread-diary.app/bread-photos/test.webp")
                .shopName(shopName)
                .eatenDate(LocalDate.of(2026, 4, 18))
                .rating(rating)
                .review("test")
                .deletedAt(deletedAt)
                .build();

        entityManager.persist(breadRecord);
    }
}
