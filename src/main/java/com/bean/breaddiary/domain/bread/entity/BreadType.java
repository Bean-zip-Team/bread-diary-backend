package com.bean.breaddiary.domain.bread.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BreadType {
    PASTRY("페이스트리"),
    BREAD("식빵"),
    DONUT("도넛"),
    CAKE("케이크"),
    BAGEL("베이글"),
    TART("타르트"),
    OTHER("기타");

    private final String label;
}
