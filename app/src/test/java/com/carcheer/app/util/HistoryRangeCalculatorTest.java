package com.carcheer.app.util;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;

public class HistoryRangeCalculatorTest {

    private static long millis(int year, int monthZeroBased, int day, int hour, int minute, int sec, int ms) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, monthZeroBased, day, hour, minute, sec);
        c.set(Calendar.MILLISECOND, ms);
        return c.getTimeInMillis();
    }

    @Test
    public void thisMonth_coversFirstToLastDay() {
        // 2026-09-25 10:30:00
        long now = millis(2026, Calendar.SEPTEMBER, 25, 10, 30, 0, 0);
        long[] r = HistoryRangeCalculator.range(HistoryRangeCalculator.TYPE_THIS_MONTH, now);
        assertEquals(millis(2026, Calendar.SEPTEMBER, 1, 0, 0, 0, 0), r[0]);
        assertEquals(millis(2026, Calendar.SEPTEMBER, 30, 23, 59, 59, 999), r[1]);
    }

    @Test
    public void thisMonth_crossYear_leapFebruary() {
        // 2024-02-10（闰年）
        long now = millis(2024, Calendar.FEBRUARY, 10, 8, 0, 0, 0);
        long[] r = HistoryRangeCalculator.range(HistoryRangeCalculator.TYPE_THIS_MONTH, now);
        assertEquals(millis(2024, Calendar.FEBRUARY, 1, 0, 0, 0, 0), r[0]);
        assertEquals(millis(2024, Calendar.FEBRUARY, 29, 23, 59, 59, 999), r[1]);
    }

    @Test
    public void last3Months_crossYear() {
        // 2026-01-15 → 近三月开始 = 2025-10-15 00:00，结束 = 2026-01-15 23:59:59.999
        long now = millis(2026, Calendar.JANUARY, 15, 12, 0, 0, 0);
        long[] r = HistoryRangeCalculator.range(HistoryRangeCalculator.TYPE_LAST_3_MONTHS, now);
        assertEquals(millis(2025, Calendar.OCTOBER, 15, 0, 0, 0, 0), r[0]);
        assertEquals(millis(2026, Calendar.JANUARY, 15, 23, 59, 59, 999), r[1]);
    }

    @Test
    public void lastHalfYear_crossYear() {
        // 2026-03-20 → 近半年开始 = 2025-09-20 00:00
        long now = millis(2026, Calendar.MARCH, 20, 18, 0, 0, 0);
        long[] r = HistoryRangeCalculator.range(HistoryRangeCalculator.TYPE_LAST_HALF_YEAR, now);
        assertEquals(millis(2025, Calendar.SEPTEMBER, 20, 0, 0, 0, 0), r[0]);
        assertEquals(millis(2026, Calendar.MARCH, 20, 23, 59, 59, 999), r[1]);
    }

    @Test
    public void boundaryInclusive_endOfDayContainsLateNightRecord() {
        // 23:59:59.999 的记录应包含在当天区间内
        long now = millis(2026, Calendar.SEPTEMBER, 25, 9, 0, 0, 0);
        long[] r = HistoryRangeCalculator.range(HistoryRangeCalculator.TYPE_THIS_MONTH, now);
        long lateNight = millis(2026, Calendar.SEPTEMBER, 25, 23, 59, 59, 999);
        assertEquals(true, lateNight >= r[0] && lateNight <= r[1]);
    }

    @Test
    public void lastYear_crossYear() {
        // 2026-09-25 → 近一年开始 = 2025-09-25 00:00
        long now = millis(2026, Calendar.SEPTEMBER, 25, 15, 0, 0, 0);
        long[] r = HistoryRangeCalculator.range(HistoryRangeCalculator.TYPE_LAST_1_YEAR, now);
        assertEquals(millis(2025, Calendar.SEPTEMBER, 25, 0, 0, 0, 0), r[0]);
        assertEquals(millis(2026, Calendar.SEPTEMBER, 25, 23, 59, 59, 999), r[1]);
    }

    @Test
    public void extremes_picksMaxPriceAndAmountWithDates() {
        com.carcheer.app.data.entity.RefuelRecord a =
                new com.carcheer.app.data.entity.RefuelRecord();
        a.date = millis(2026, Calendar.SEPTEMBER, 1, 8, 0, 0, 0);
        a.price = 8.63;
        a.amount = 350.0;
        com.carcheer.app.data.entity.RefuelRecord b =
                new com.carcheer.app.data.entity.RefuelRecord();
        b.date = millis(2026, Calendar.SEPTEMBER, 25, 8, 0, 0, 0);
        b.price = 7.80;
        b.amount = 400.0;
        // 并列最高价，时间更晚的 b 应胜出（记录按时间正序传入）
        com.carcheer.app.data.entity.RefuelRecord c =
                new com.carcheer.app.data.entity.RefuelRecord();
        c.date = millis(2026, Calendar.SEPTEMBER, 20, 8, 0, 0, 0);
        c.price = 7.80;
        c.amount = null;

        java.util.List<com.carcheer.app.data.entity.RefuelRecord> asc =
                java.util.Arrays.asList(a, c, b);
        StatsCalculator.RangeExtremes ex = StatsCalculator.extremes(asc);
        assertEquals(8.63, ex.maxPrice, 0.0001);
        assertEquals(a.date, (long) ex.maxPriceDate);
        assertEquals(400.0, ex.maxAmount, 0.0001);
        assertEquals(b.date, (long) ex.maxAmountDate);
    }

    @Test
    public void extremes_emptyOrNullList() {
        assertEquals(null, StatsCalculator.extremes(null).maxPrice);
        assertEquals(null, StatsCalculator.extremes(
                new java.util.ArrayList<com.carcheer.app.data.entity.RefuelRecord>()).maxAmount);
    }
}
