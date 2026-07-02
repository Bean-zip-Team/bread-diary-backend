package com.bean.breaddiary.domain.onboarding.dto.mapper;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadtype.BreadTypeTestFixture;
import com.bean.breaddiary.domain.onboarding.dto.response.OnboardingBreadItemResponse;
import com.bean.breaddiary.domain.onboarding.entity.OnboardingBreadCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OnboardingMapperTest {

    private OnboardingMapper onboardingMapper;

    @BeforeEach
    void setUp() {
        onboardingMapper = Mappers.getMapper(OnboardingMapper.class);
    }

    @Test
    void mapToItemConvertsCatalogPngToWebpForOnboarding() {
        Bread bread = bread("https://du4zizlgiw14n.cloudfront.net/images/041_%EC%83%9D%EC%8B%9D%EB%B9%B5.png");

        OnboardingBreadItemResponse response = onboardingMapper.mapToItem(OnboardingBreadCatalog.RED_BEAN_BREAD, bread);

        assertEquals("https://du4zizlgiw14n.cloudfront.net/images/041_%EC%83%9D%EC%8B%9D%EB%B9%B5.webp", response.getImageUrl());
    }

    @Test
    void mapToItemKeepsPlaceholderPngUnchanged() {
        Bread bread = bread("https://du4zizlgiw14n.cloudfront.net/images/041_%EC%83%9D%EC%8B%9D%EB%B9%B5_placeholder.png");

        OnboardingBreadItemResponse response = onboardingMapper.mapToItem(OnboardingBreadCatalog.RED_BEAN_BREAD, bread);

        assertEquals("https://du4zizlgiw14n.cloudfront.net/images/041_%EC%83%9D%EC%8B%9D%EB%B9%B5_placeholder.png", response.getImageUrl());
    }

    @Test
    void toOnboardingImageUrlReturnsNullWhenImageUrlIsNull() {
        assertNull(onboardingMapper.toOnboardingImageUrl(null));
    }

    private Bread bread(String imageUrl) {
        return Bread.builder()
                .id(UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789"))
                .stickerNumber(41)
                .name("생식빵")
                .breadType(BreadTypeTestFixture.BREAD)
                .imageUrl(imageUrl)
                .build();
    }
}
