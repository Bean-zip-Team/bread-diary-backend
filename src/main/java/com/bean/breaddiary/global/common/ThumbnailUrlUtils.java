package com.bean.breaddiary.global.common;

public final class ThumbnailUrlUtils {

    private static final String THUMBNAIL_SUFFIX = "_thumb";

    private ThumbnailUrlUtils() {
    }

    public static String toThumbnailUrl(String photoUrl) {
        if (photoUrl == null) {
            return null;
        }

        int extensionIndex = photoUrl.lastIndexOf('.');
        if (extensionIndex < 0) {
            return photoUrl + THUMBNAIL_SUFFIX;
        }

        return photoUrl.substring(0, extensionIndex)
                + THUMBNAIL_SUFFIX
                + photoUrl.substring(extensionIndex);
    }
}
