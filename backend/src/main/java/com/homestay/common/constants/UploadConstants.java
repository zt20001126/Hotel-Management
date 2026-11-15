package com.homestay.common.constants;

/** File upload limits and URL constants. */
public final class UploadConstants {

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    public static final long MAX_REQUEST_SIZE_BYTES = 50L * 1024 * 1024;
    public static final String URL_PREFIX = "/uploads";

    private UploadConstants() {
    }
}
