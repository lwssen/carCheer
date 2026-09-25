package com.carcheer.app.util;

import java.util.Calendar;

/** 历史查询时间类型 → 开始/结束时间戳（毫秒）反算 */
public final class HistoryRangeCalculator {

    public static final int TYPE_THIS_MONTH = 0;
    public static final int TYPE_LAST_3_MONTHS = 1;
    public static final int TYPE_LAST_HALF_YEAR = 2;
    public static final int TYPE_CUSTOM = 3;
    public static final int TYPE_LAST_1_YEAR = 4;

    private HistoryRangeCalculator() {
    }

    /**
     * @return long[2]{开始毫秒(00:00:00.000), 结束毫秒(23:59:59.999)}
     */
    public static long[] range(int type, long nowMillis) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(nowMillis);
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(nowMillis);
        switch (type) {
            case TYPE_THIS_MONTH:
                start.set(Calendar.DAY_OF_MONTH, 1);
                setStartOfDay(start);
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
                setEndOfDay(end);
                break;
            case TYPE_LAST_3_MONTHS:
                start.add(Calendar.MONTH, -3);
                setStartOfDay(start);
                setEndOfDay(end);
                break;
            case TYPE_LAST_HALF_YEAR:
                start.add(Calendar.MONTH, -6);
                setStartOfDay(start);
                setEndOfDay(end);
                break;
            case TYPE_LAST_1_YEAR:
                start.add(Calendar.MONTH, -12);
                setStartOfDay(start);
                setEndOfDay(end);
                break;
            default:
                // 自定义不由本类计算
                setStartOfDay(start);
                setEndOfDay(end);
                break;
        }
        return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
    }

    private static void setStartOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private static void setEndOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
    }
}
