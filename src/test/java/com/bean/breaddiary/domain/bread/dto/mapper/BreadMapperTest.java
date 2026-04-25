package com.bean.breaddiary.domain.bread.dto.mapper;

import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteItemResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadCatalogItemResponse;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadtype.entity.BreadType;
import com.bean.breaddiary.domain.breadrecord.dto.projection.BreadRecordCatalogStats;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.UUID;

import static com.bean.breaddiary.domain.breadtype.BreadTypeTestFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BreadMapperTest {

    private final BreadMapper breadMapper = Mappers.getMapper(BreadMapper.class);

    @Test
    void mapToBreadDoesNotConvertNameOrImageUrlToThumbnail() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        CreateNewBreadRecordRequest request = new CreateNewBreadRecordRequest();
        request.setName("쫀득쫀득 두쫀쿠");
        request.setBreadType("PASTRY");

        Bread bread = breadMapper.mapToBread(
                request,
                PASTRY,
                1,
                "https://cdn.bread-diary.app/catalog/default_user_bread.webp",
                userId
        );

        assertEquals("쫀득쫀득 두쫀쿠", bread.getName());
        assertEquals("https://cdn.bread-diary.app/catalog/default_user_bread.webp", bread.getImageUrl());
        assertEquals(PASTRY, bread.getBreadType());
        assertEquals(userId, bread.getCreatedBy());
    }

    @Test
    void mapToCatalogItemOnlyConvertsLatestPhotoUrlToThumbnail() {
        UUID breadId = UUID.fromString("b78af3bc-d52b-4aa9-9996-182003d018ba");
        Bread bread = Bread.builder()
                .id(breadId)
                .stickerNumber(1)
                .name("쫀득쫀득 두쫀쿠")
                .breadType(PASTRY)
                .imageUrl("https://cdn.bread-diary.app/catalog/default_user_bread.webp")
                .build();
        BreadRecordCatalogStats stats = new BreadRecordCatalogStats(
                breadId,
                1L,
                5.0,
                "https://breaddiary-bucket.s3.ap-northeast-2.amazonaws.com/bread/photo.jpg",
                LocalDate.of(2026, 4, 18)
        );

        BreadCatalogItemResponse response = breadMapper.mapToCatalogItem(bread, stats);

        assertEquals("쫀득쫀득 두쫀쿠", response.getName());
        assertEquals("https://cdn.bread-diary.app/catalog/default_user_bread.webp", response.getImageUrl());
        assertEquals(
                "https://breaddiary-bucket.s3.ap-northeast-2.amazonaws.com/bread/photo_thumb.jpg",
                response.getLatestPhotoUrl()
        );
    }

    @Test
    void mapToCatalogItemUsesPlaceholderImageForUncollectedBread() {
        UUID breadId = UUID.fromString("b78af3bc-d52b-4aa9-9996-182003d018ba");
        Bread bread = Bread.builder()
                .id(breadId)
                .stickerNumber(1)
                .name("쫀득쫀득 두쫀쿠")
                .breadType(PASTRY)
                .imageUrl("https://cdn.bread-diary.app/breads/croissant.webp")
                .build();

        BreadCatalogItemResponse response = breadMapper.mapToCatalogItem(bread, null);

        assertEquals("https://cdn.bread-diary.app/breads/croissant_placeholder.webp", response.getImageUrl());
        assertEquals(false, response.getIsCollected());
        assertEquals(0L, response.getEatCount());
    }

    @Test
    void mapToAutocompleteItemKeepsOriginalImageForCollectedBread() {
        UUID breadId = UUID.fromString("b78af3bc-d52b-4aa9-9996-182003d018ba");
        Bread bread = Bread.builder()
                .id(breadId)
                .stickerNumber(1)
                .name("쫀득쫀득 두쫀쿠")
                .breadType(PASTRY)
                .imageUrl("https://cdn.bread-diary.app/breads/croissant.webp")
                .build();

        BreadAutocompleteItemResponse response = breadMapper.mapToAutocompleteItem(bread, 2L);

        assertEquals("https://cdn.bread-diary.app/breads/croissant.webp", response.getImageUrl());
        assertEquals(2L, response.getEatCount());
    }

    @Test
    void mapToAutocompleteItemUsesPlaceholderImageForUncollectedBread() {
        UUID breadId = UUID.fromString("b78af3bc-d52b-4aa9-9996-182003d018ba");
        Bread bread = Bread.builder()
                .id(breadId)
                .stickerNumber(1)
                .name("쫀득쫀득 두쫀쿠")
                .breadType(PASTRY)
                .imageUrl("https://cdn.bread-diary.app/breads/croissant.webp")
                .build();

        BreadAutocompleteItemResponse response = breadMapper.mapToAutocompleteItem(bread, 0L);

        assertEquals("https://cdn.bread-diary.app/breads/croissant_placeholder.webp", response.getImageUrl());
        assertEquals(0L, response.getEatCount());
    }
}
