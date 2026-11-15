package com.homestay.common.constants;

import java.util.concurrent.TimeUnit;

/** 登录令牌有效期配置，区分宾客与员工。 */
public final class JwtConstants {

    public static final long EMPLOYEE_TOKEN_EXPIRATION_MILLIS = TimeUnit.DAYS.toMillis(1);
    public static final long GUEST_TOKEN_EXPIRATION_MILLIS = TimeUnit.DAYS.toMillis(30);
    public static final String GUEST_USER_TYPE = "guest";

    private JwtConstants() {
    }
}
