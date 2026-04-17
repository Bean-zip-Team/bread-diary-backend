package com.bean.breaddiary.domain.breadrecord.dto.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BreadRecordCatalogStats {

    private UUID breadId;

    private Long eatCount;

    private Double avgRating;

    private String latestPhotoUrl;

    private LocalDate latestEatenDate;
}
