package com.bean.breaddiary.domain.onboarding.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum OnboardingBreadCatalog {

    RED_BEAN_BREAD("단팥빵"),
    SALT_BREAD("소금빵"),
    DUBAI_CHEWY_COOKIE("두쫀쿠"),
    SOBORO_BREAD("소보로빵"),
    CHESTNUT_LOAF("밤식빵"),
    CREAM_PUFF_BREAD("슈크림빵"),
    MACARON("마카롱"),
    DONUT("도넛"),
    CROISSANT("크루아상");

    private final String catalogName;

    public static List<String> catalogNames() {
        return Arrays.stream(values())
                .map(OnboardingBreadCatalog::getCatalogName)
                .toList();
    }
}
