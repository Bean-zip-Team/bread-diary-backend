package com.bean.breaddiary.domain.bread.dto.mapper;

import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

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
}
