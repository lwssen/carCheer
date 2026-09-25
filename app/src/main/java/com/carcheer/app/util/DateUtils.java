package com.carcheer.app.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {

    private static final String PATTERN = "yyyy-MM-dd HH:mm";

    private DateUtils() {
    }

    public static String format(long millis) {
        return new SimpleDateFormat(PATTERN, Locale.CHINA).format(new Date(millis));
    }

    /** 与 format 对应的解析，失败返回 0 */
    public static long parse(String text) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(PATTERN, Locale.CHINA);
            format.setLenient(false);
            Date date = format.parse(text.trim());
            return date == null ? 0 : date.getTime();
        } catch (Exception e) {
            return 0;
        }
    }
}
