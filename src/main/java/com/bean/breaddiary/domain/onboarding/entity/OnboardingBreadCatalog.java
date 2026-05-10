package com.bean.breaddiary.domain.onboarding.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum OnboardingBreadCatalog {

    LOAF_BREAD("생식빵"),
    SALT_BREAD("소금빵"),
    SOBORO_BREAD("소보로빵"),
    RED_BEAN_BREAD("단팥빵"),
    SAUSAGE_BREAD("소시지빵"),
    ROLL_CAKE("롤 케이크"),
    CROISSANT("크루아상"),
    DONUT("도넛"),
    CASTELLA("카스테라"),
    BAGEL("베이글"),
    TWIST_DONUT("꽈배기"),
    CREAM_PUFF_BREAD("슈크림빵"),
    GARLIC_BAGUETTE("마늘바게트"),
    BAGUETTE("바게트"),
    WAFFLE("와플"),
    CROQUETTE("고로케"),
    PANCAKE("팬케이크"),
    EGG_TART("에그타르트"),
    MACARON("마카롱"),
    CHESTNUT_LOAF("밤식빵"),
    MUFFIN("머핀"),
    MOCHA_BUN("모카번"),
    ANBUTTER("앙버터"),
    SCONE("스콘"),
    BROWNIE("브라우니"),
    MAMMOTH_BREAD("맘모스빵"),
    CIABATTA("치아바타"),
    CUPCAKE("컵케이크"),
    HONEY_BREAD("허니브레드"),
    PRETZEL("프레첼");

    private final String catalogName;

    public static List<String> catalogNames() {
        return Arrays.stream(values())
                .map(OnboardingBreadCatalog::getCatalogName)
                .toList();
    }
}
