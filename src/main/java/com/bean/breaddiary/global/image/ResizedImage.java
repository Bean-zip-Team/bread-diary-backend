package com.bean.breaddiary.global.image;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResizedImage {

    private byte[] bytes;
    private String contentType;
    private String extension;
}
