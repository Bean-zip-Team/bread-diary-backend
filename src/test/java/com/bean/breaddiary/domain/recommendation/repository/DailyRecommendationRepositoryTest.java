package com.bean.breaddiary.domain.recommendation.repository;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.recommendation.entity.DailyRecommendation;
import com.bean.breaddiary.domain.breadtype.entity.BreadType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class DailyRecommendationRepositoryTest {

    @Autowired
    private DailyRecommendationRepository dailyRecommendationRepository;

    @Autowired
    private EntityManager entityManager;

    private BreadType breadType;

    @BeforeEach
    void setUp() {
        breadType = BreadType.builder()
                .code("PASTRY")
                .name("페이스트리")
                .build();
        entityManager.persist(breadType);
    }

    @Test
    void findAllByRecommendationDateOrderByDisplayOrderAscReturnsOrderedItems() {
        LocalDate today = LocalDate.of(2026, 4, 26);
        Bread secondBread = saveBread("두번째", 2);
        Bread firstBread = saveBread("첫번째", 1);

        saveRecommendation(today, secondBread, 2);
        saveRecommendation(today, firstBread, 1);

        List<DailyRecommendation> recommendations =
                dailyRecommendationRepository.findAllByRecommendationDateOrderByDisplayOrderAsc(today);

        assertEquals(2, recommendations.size());
        assertEquals(firstBread.getId(), recommendations.get(0).getBread().getId());
        assertEquals(secondBread.getId(), recommendations.get(1).getBread().getId());
    }

    @Test
    void findDistinctBreadIdsByRecommendationDateBetweenReturnsRecentBreadIds() {
        Bread firstBread = saveBread("소금빵", 1);
        Bread secondBread = saveBread("크루아상", 2);
        Bread thirdBread = saveBread("베이글", 3);

        saveRecommendation(LocalDate.of(2026, 4, 24), firstBread, 1);
        saveRecommendation(LocalDate.of(2026, 4, 25), secondBread, 1);
        saveRecommendation(LocalDate.of(2026, 4, 26), thirdBread, 1);

        List<UUID> breadIds = dailyRecommendationRepository.findDistinctBreadIdsByRecommendationDateBetween(
                LocalDate.of(2026, 4, 24),
                LocalDate.of(2026, 4, 25)
        );

        assertEquals(2, breadIds.size());
        assertTrue(breadIds.contains(firstBread.getId()));
        assertTrue(breadIds.contains(secondBread.getId()));
    }

    private Bread saveBread(String name, int stickerNumber) {
        Bread bread = Bread.builder()
                .stickerNumber(stickerNumber)
                .name(name + "-" + UUID.randomUUID().toString().substring(0, 8))
                .breadType(breadType)
                .imageUrl("https://cdn.bread-diary.app/breads/test.webp")
                .build();

        entityManager.persist(bread);
        return bread;
    }

    private void saveRecommendation(LocalDate date, Bread bread, int displayOrder) {
        entityManager.persist(DailyRecommendation.builder()
                .recommendationDate(date)
                .bread(bread)
                .displayOrder(displayOrder)
                .build());
    }
}
