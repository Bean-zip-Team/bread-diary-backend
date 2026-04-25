package com.bean.breaddiary.domain.breadrecord.repository;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadtype.entity.BreadType;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCountProjection;
import com.bean.breaddiary.domain.breadrecord.dto.projection.UserStatsProjection;
import com.bean.breaddiary.domain.breadrecord.entity.BreadRecord;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.bean.breaddiary.domain.breadtype.BreadTypeTestFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class BreadRecordRepositoryTest {

    @Autowired
    private BreadRecordRepository breadRecordRepository;

    @Autowired
    private EntityManager entityManager;

    private BreadType breadType;

    @BeforeEach
    void setUp() {
        breadType = breadType(null, "BREAD", "식빵");
        entityManager.persist(breadType);
    }

    @Test
    void findUserStatsByUserIdExcludesNullAndBlankShopNames() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Bread bread = saveBread();

        saveBreadRecord(userId, bread, "shop-a", 5, LocalDate.of(2026, 4, 18), null);
        saveBreadRecord(userId, bread, "   ", 4, LocalDate.of(2026, 4, 18), null);
        saveBreadRecord(userId, bread, "", 3, LocalDate.of(2026, 4, 18), null);
        saveBreadRecord(userId, bread, null, 2, LocalDate.of(2026, 4, 18), null);
        saveBreadRecord(userId, bread, "shop-b", 1, LocalDate.of(2026, 4, 18), null);
        saveBreadRecord(userId, bread, "shop-a", 5, LocalDate.of(2026, 4, 18), null);

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

        saveBreadRecord(userId, bread, "shop-live", 5, LocalDate.of(2026, 4, 18), null);
        saveBreadRecord(userId, bread, "shop-deleted", 1, LocalDate.of(2026, 4, 18), LocalDateTime.of(2026, 4, 18, 12, 0));

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

        saveBreadRecord(userId, firstBread, "shop-a", 5, LocalDate.of(2026, 4, 18), null);
        saveBreadRecord(userId, firstBread, "shop-a", 4, LocalDate.of(2026, 4, 18), null);
        saveBreadRecord(userId, secondBread, "shop-b", 3, LocalDate.of(2026, 4, 18), null);
        saveBreadRecord(userId, thirdBread, "shop-c", 2, LocalDate.of(2026, 4, 18), null);
        saveBreadRecord(userId, deletedOnlyBread, "shop-d", 1, LocalDate.of(2026, 4, 18), LocalDateTime.of(2026, 4, 18, 12, 0));

        UserStatsProjection stats = breadRecordRepository.findUserStatsByUserId(userId);

        assertNotNull(stats);
        assertEquals(4L, stats.getTotalRecords());
        assertEquals(3L, stats.getTotalStickers());
        assertEquals(3L, stats.getUniqueShops());
        assertEquals(3.5, stats.getAvgRating());
    }

    @Test
    void countRecentActiveSystemRecordsByBreadUsesEatenDateAndIgnoresDeletedAndUserCreatedBread() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
        Bread countedBread = saveBread(10);
        Bread oldBread = saveBread(11);
        Bread deletedBread = saveBread(12);
        Bread userCreatedBread = saveUserCreatedBread(13);

        saveBreadRecord(userId, countedBread, "shop-a", 5, LocalDate.of(2026, 4, 23), null);
        saveBreadRecord(userId, countedBread, "shop-b", 4, LocalDate.of(2026, 4, 25), null);
        saveBreadRecord(userId, oldBread, "shop-c", 3, LocalDate.of(2026, 4, 10), null);
        saveBreadRecord(userId, deletedBread, "shop-d", 2, LocalDate.of(2026, 4, 24), LocalDateTime.of(2026, 4, 24, 12, 0));
        saveBreadRecord(userId, userCreatedBread, "shop-e", 1, LocalDate.of(2026, 4, 24), null);

        List<BreadRecordCountProjection> counts = breadRecordRepository.countRecentActiveSystemRecordsByBread(
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26)
        );

        assertEquals(1, counts.size());
        assertEquals(countedBread.getId(), counts.get(0).getBreadId());
        assertEquals(2L, counts.get(0).getEatCount());
    }

    @Test
    void countTotalActiveRecordsByBreadIdsIgnoresSoftDeletedRecords() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440011");
        Bread countedBread = saveBread(20);
        Bread zeroBread = saveBread(21);

        saveBreadRecord(userId, countedBread, "shop-a", 5, LocalDate.of(2026, 4, 20), null);
        saveBreadRecord(userId, countedBread, "shop-b", 4, LocalDate.of(2026, 4, 21), null);
        saveBreadRecord(userId, countedBread, "shop-c", 3, LocalDate.of(2026, 4, 22), LocalDateTime.of(2026, 4, 22, 12, 0));

        List<BreadRecordCountProjection> counts = breadRecordRepository.countTotalActiveRecordsByBreadIds(
                List.of(countedBread.getId(), zeroBread.getId())
        );

        assertEquals(1, counts.size());
        assertEquals(countedBread.getId(), counts.get(0).getBreadId());
        assertEquals(2L, counts.get(0).getEatCount());
        assertTrue(counts.stream().noneMatch(count -> count.getBreadId().equals(zeroBread.getId())));
    }

    private Bread saveBread() {
        return saveBread(1);
    }

    private Bread saveBread(int stickerNumber) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Bread bread = Bread.builder()
                .stickerNumber(stickerNumber)
                .name("bread-" + suffix)
                .breadType(breadType)
                .imageUrl("https://cdn.bread-diary.app/breads/test.webp")
                .build();

        entityManager.persist(bread);
        return bread;
    }

    private Bread saveUserCreatedBread(int stickerNumber) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Bread bread = Bread.builder()
                .stickerNumber(stickerNumber)
                .name("user-bread-" + suffix)
                .breadType(breadType)
                .imageUrl("https://cdn.bread-diary.app/breads/test.webp")
                .createdBy(UUID.fromString("550e8400-e29b-41d4-a716-446655440999"))
                .build();

        entityManager.persist(bread);
        return bread;
    }

    private void saveBreadRecord(
            UUID userId,
            Bread bread,
            String shopName,
            int rating,
            LocalDate eatenDate,
            LocalDateTime deletedAt
    ) {
        BreadRecord breadRecord = BreadRecord.builder()
                .userId(userId)
                .bread(bread)
                .photoUrl("https://cdn.bread-diary.app/bread-photos/test.webp")
                .shopName(shopName)
                .eatenDate(eatenDate)
                .rating(rating)
                .review("test")
                .deletedAt(deletedAt)
                .build();

        entityManager.persist(breadRecord);
    }
}
