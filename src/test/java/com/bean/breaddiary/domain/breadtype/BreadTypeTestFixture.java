package com.bean.breaddiary.domain.breadtype;

import com.bean.breaddiary.domain.breadtype.entity.BreadType;

public final class BreadTypeTestFixture {

    public static final BreadType PASTRY = breadType(1L, "PASTRY", "페이스트리");
    public static final BreadType BREAD = breadType(2L, "BREAD", "식빵");
    public static final BreadType BAGEL = breadType(3L, "BAGEL", "베이글");
    public static final BreadType SWEET_BREAD = breadType(4L, "SWEET_BREAD", "단과자빵");

    private BreadTypeTestFixture() {
    }

    public static BreadType breadType(Long id, String code, String name) {
        return BreadType.builder()
                .id(id)
                .code(code)
                .name(name)
                .build();
    }
}
