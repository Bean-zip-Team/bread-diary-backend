package com.bean.breaddiary.domain.bread.service;

import com.bean.breaddiary.domain.bread.dto.mapper.BreadMapper;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.repository.BreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static com.bean.breaddiary.domain.breadtype.BreadTypeTestFixture.BAGEL;
import static com.bean.breaddiary.domain.breadtype.BreadTypeTestFixture.PASTRY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class BreadServiceTest {

    private BreadRepository breadRepository;
    private BreadMapper breadMapper;
    private BreadService breadService;

    @BeforeEach
    void setUp() {
        breadRepository = mock(BreadRepository.class);
        breadMapper = mock(BreadMapper.class);
        breadService = new BreadService(breadRepository, breadMapper);
    }

    @Test
    void searchAutocompleteBreadsUsesStickerCursorForQuerySearch() {
        Bread firstBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                4,
                "크루키",
                PASTRY
        );
        Bread secondBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                6,
                "크루아상",
                PASTRY
        );

        when(breadRepository.findAutocompleteByNameContainingAfterStickerNumber(
                "크루",
                3,
                PageRequest.of(0, 2)
        )).thenReturn(List.of(firstBread, secondBread));

        BreadService.AutocompleteSlice actual = breadService.searchAutocompleteBreads("크루", "3", 1);

        assertEquals(List.of(firstBread), actual.getItems());
        assertEquals("4", actual.getNextCursor());
        assertTrue(actual.getHasMore());
    }

    @Test
    void searchAutocompleteBreadsUsesPopularCursorWhenQueryIsBlank() {
        Bread firstBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                4,
                "소금빵",
                PASTRY
        );
        Bread secondBread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                8,
                "베이글",
                BAGEL
        );

        when(breadRepository.findPopularAutocompleteAfterCursor(
                12L,
                3,
                PageRequest.of(0, 2)
        )).thenReturn(List.of(firstBread, secondBread));
        when(breadRepository.countActiveRecordsByBreadId(firstBread.getId())).thenReturn(11L);

        BreadService.AutocompleteSlice actual = breadService.searchAutocompleteBreads("  ", "12_3", 1);

        assertEquals(List.of(firstBread), actual.getItems());
        assertEquals("11_4", actual.getNextCursor());
        assertTrue(actual.getHasMore());
        verify(breadRepository).countActiveRecordsByBreadId(firstBread.getId());
    }

    @Test
    void searchAutocompleteBreadsReturnsNoCursorWhenPageEnds() {
        Bread bread = createBread(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                4,
                "소금빵",
                PASTRY
        );

        when(breadRepository.findAutocompleteByNameContainingAfterStickerNumber(
                "소금",
                null,
                PageRequest.of(0, 21)
        )).thenReturn(List.of(bread));

        BreadService.AutocompleteSlice actual = breadService.searchAutocompleteBreads("소금", null, null);

        assertEquals(List.of(bread), actual.getItems());
        assertNull(actual.getNextCursor());
        assertFalse(actual.getHasMore());
        verifyNoMoreInteractions(breadMapper);
    }

    private Bread createBread(UUID id, int stickerNumber, String name, com.bean.breaddiary.domain.breadtype.entity.BreadType breadType) {
        return Bread.builder()
                .id(id)
                .stickerNumber(stickerNumber)
                .name(name)
                .breadType(breadType)
                .imageUrl("https://cdn.bread-diary.app/breads/" + name + ".webp")
                .build();
    }
}
