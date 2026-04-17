package com.bean.breaddiary.domain.bread.dto.mapper;

import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteItemResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteResponse;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BreadMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stickerNumber", source = "stickerNumber")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "breadType", source = "request.breadType")
    @Mapping(target = "imageUrl", source = "imageUrl")
    @Mapping(target = "createdBy", source = "userId")
    Bread mapToBread(CreateNewBreadRecordRequest request, Integer stickerNumber, String imageUrl, UUID userId);

    @Mapping(target = "breadId", source = "bread.id")
    @Mapping(target = "name", source = "bread.name")
    @Mapping(target = "breadType", source = "bread.breadType")
    @Mapping(target = "stickerNumber", source = "bread.stickerNumber")
    @Mapping(target = "imageUrl", source = "bread.imageUrl")
    @Mapping(target = "eatCount", expression = "java(resolveEatCount(eatCount))")
    BreadAutocompleteItemResponse mapToAutocompleteItem(Bread bread, Long eatCount);

    default BreadAutocompleteResponse mapToAutocompleteResponse(List<Bread> breads, Map<UUID, Long> eatCounts) {
        List<BreadAutocompleteItemResponse> items = breads.stream()
                .map(bread -> mapToAutocompleteItem(
                        bread,
                        eatCounts.get(bread.getId())
                ))
                .toList();

        return new BreadAutocompleteResponse(items);
    }

    default Long resolveEatCount(Long eatCount) {
        return eatCount == null ? 0L : eatCount;
    }
}
