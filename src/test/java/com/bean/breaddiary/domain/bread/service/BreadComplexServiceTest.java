package com.bean.breaddiary.domain.bread.service;

import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteResponse;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.entity.BreadType;
import com.bean.breaddiary.domain.breadrecord.service.BreadRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BreadComplexServiceTest {

    private BreadService breadService;
    private BreadRecordService breadRecordService;
    private BreadComplexService breadComplexService;

    @BeforeEach
    void setUp() {
        breadService = mock(BreadService.class);
        breadRecordService = mock(BreadRecordService.class);
        breadComplexService = new BreadComplexService(breadService, breadRecordService);
    }

    @Test
    void autocompleteBreadsCombinesBreadSearchAndUserEatCounts() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID breadId = UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789");
        Bread bread = Bread.builder()
                .id(breadId)
                .stickerNumber(6)
                .name("크루아상")
                .breadType(BreadType.PASTRY)
                .imageUrl("https://cdn.bread-diary.app/breads/croissant.webp")
                .build();
        BreadAutocompleteResponse expected = new BreadAutocompleteResponse(List.of());

        when(breadService.searchAutocompleteBreads("크루")).thenReturn(List.of(bread));
        when(breadRecordService.countActiveRecordsByBreadIds(userId, List.of(breadId)))
                .thenReturn(Map.of(breadId, 5L));
        when(breadService.createAutocompleteResponse(List.of(bread), Map.of(breadId, 5L)))
                .thenReturn(expected);

        BreadAutocompleteResponse actual = breadComplexService.autocompleteBreads("크루", userId);

        assertSame(expected, actual);
        verify(breadService).searchAutocompleteBreads("크루");
        verify(breadRecordService).countActiveRecordsByBreadIds(userId, List.of(breadId));
        verify(breadService).createAutocompleteResponse(List.of(bread), Map.of(breadId, 5L));
    }

    @Test
    void autocompleteBreadsSkipsEatCountLookupForAnonymousUser() {
        BreadAutocompleteResponse expected = new BreadAutocompleteResponse(List.of());

        when(breadService.searchAutocompleteBreads(null)).thenReturn(List.of());
        when(breadService.createAutocompleteResponse(List.of(), Map.of())).thenReturn(expected);

        BreadAutocompleteResponse actual = breadComplexService.autocompleteBreads(null, null);

        assertSame(expected, actual);
        verify(breadService).searchAutocompleteBreads(null);
        verifyNoInteractions(breadRecordService);
        verify(breadService).createAutocompleteResponse(List.of(), Map.of());
    }
}
