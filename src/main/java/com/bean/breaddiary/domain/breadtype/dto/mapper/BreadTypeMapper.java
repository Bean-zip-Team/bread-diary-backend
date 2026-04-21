package com.bean.breaddiary.domain.breadtype.dto.mapper;

import com.bean.breaddiary.domain.breadtype.dto.response.BreadTypeItemResponse;
import com.bean.breaddiary.domain.breadtype.dto.response.BreadTypeListResponse;
import com.bean.breaddiary.domain.breadtype.entity.BreadType;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BreadTypeMapper {

    default BreadTypeListResponse mapToBreadTypeListResponse(List<BreadType> breadTypes) {
        List<BreadTypeItemResponse> items = breadTypes.stream()
                .map(this::mapToBreadTypeItem)
                .toList();

        return new BreadTypeListResponse(items);
    }

    default BreadTypeItemResponse mapToBreadTypeItem(BreadType breadType) {
        return new BreadTypeItemResponse(
                breadType.getCode(),
                breadType.getName()
        );
    }
}
