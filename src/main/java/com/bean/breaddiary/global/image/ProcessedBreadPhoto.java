package com.bean.breaddiary.global.image;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProcessedBreadPhoto {

    private ResizedImage original;
    private ResizedImage thumbnail;
}
