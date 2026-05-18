package com.bean.breaddiary.domain.onboarding.dto.mapper;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.onboarding.entity.OnboardingBreadCatalog;
import com.bean.breaddiary.domain.onboarding.dto.response.OnboardingBreadItemResponse;
import com.bean.breaddiary.domain.onboarding.dto.response.OnboardingBreadListResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OnboardingMapper {

    @Mapping(target = "breadId", source = "bread.id")
    @Mapping(target = "name", source = "bread.name")
    @Mapping(target = "stickerNumber", source = "bread.stickerNumber")
    @Mapping(target = "breadType", source = "bread.breadType.code")
    @Mapping(target = "imageUrl", expression = "java(toOnboardingImageUrl(bread.getImageUrl()))")
    OnboardingBreadItemResponse mapToItem(OnboardingBreadCatalog catalog, Bread bread);

    default OnboardingBreadListResponse mapToListResponse(List<OnboardingBreadItemResponse> items) {
        return new OnboardingBreadListResponse(items);
    }

    default String toOnboardingImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.endsWith("_placeholder.png")) {
            return imageUrl;
        }

        if (imageUrl.endsWith(".png")) {
            return imageUrl.substring(0, imageUrl.length() - 4) + ".webp";
        }

        return imageUrl;
    }
}
