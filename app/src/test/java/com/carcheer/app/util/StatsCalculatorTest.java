package com.carcheer.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.carcheer.app.data.entity.RefuelRecord;
import com.carcheer.app.util.FuelCalculator.Result;
import com.carcheer.app.util.StatsCalculator.MonthStat;
import com.carcheer.app.util.StatsCalculator.Overview;
import com.carcheer.app.util.StatsCalculator.RangeExtremes;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatsCalculatorTest {

    private static RefuelRecord record(long id, long date, double odometer,
                                       Double amount, Double volume, Double price, boolean fullTank) {
        RefuelRecord r = new RefuelRecord();
        r.id = id;
        r.vehicleId = 1;
        r.date = date;
        r.odometer = odometer;
        r.amount = amount;
        r.volume = volume;
        r.price = price;
        r.fullTank = fullTank;
        return r;
    }

    /** 概览：次数/金额/油量汇总与加权平均油耗 */
    @Test
    public void overview_sumsAndWeightedAverage() {
        // 记录2 缺油量无法结算；记录3 以记录2为基准：40L/300km=13.33；记录4 以记录3为基准：20L/500km=4.0
        // 加权平均 = (40+20)/(300+500)*100 = 7.5（简单平均 8.67，可区分两种算法）
        List<RefuelRecord> records = Arrays.asList(
                record(1, 1000L, 10000, 300.0, 40.0, 7.5, true),
                record(2, 2000L, 10200, null, null, 7.8, true),
                record(3, 3000L, 10500, 200.0, 40.0, 7.8, true),
                record(4, 4000L, 11000, null, 20.0, 8.0, true));
        Result fr = FuelCalculator.calculate(records);

        Overview ov = StatsCalculator.overview(records, fr);

        assertEquals(4, ov.count);
        assertEquals(500.0, ov.totalAmount, 0.0001);
        assertEquals(100.0, ov.totalVolume, 0.0001);
        assertEquals(7.5, ov.avgConsumption, 0.0001);
        assertEquals(8.0, ov.latestPrice, 0.0001);
        assertEquals(7.8, ov.previousPrice, 0.0001);
    }

    /** 没有任何可结算区间时平均油耗为 null */
    @Test
    public void overview_noSettledSegments() {
        List<RefuelRecord> records = Arrays.asList(
                record(1, 1000L, 10000, 300.0, 40.0, 7.5, true));
        Result fr = FuelCalculator.calculate(records);
        Overview ov = StatsCalculator.overview(records, fr);

        assertNull(ov.avgConsumption);
        assertEquals(7.5, ov.latestPrice, 0.0001);
        assertNull(ov.previousPrice);
    }

    /** 极值：最高/最低油价与最高金额及日期，并列时取最近一次；缺油价记录不参与油价极值 */
    @Test
    public void extremes_maxMinPriceAndMaxAmount() {
        List<RefuelRecord> records = Arrays.asList(
                record(1, 1000L, 10000, 300.0, 40.0, 7.8, true),
                record(2, 2000L, 10500, 200.0, 40.0, 7.5, true),
                record(3, 3000L, 11000, 250.0, 40.0, 7.8, true),   // 油价并列最高，取最近
                record(4, 4000L, 11500, null, null, null, true));  // 缺油价/金额
        RangeExtremes ex = StatsCalculator.extremes(records);

        assertEquals(7.8, ex.maxPrice, 0.0001);
        assertEquals(3000L, ex.maxPriceDate.longValue());
        assertEquals(7.5, ex.minPrice, 0.0001);
        assertEquals(2000L, ex.minPriceDate.longValue());
        assertEquals(300.0, ex.maxAmount, 0.0001);
        assertEquals(1000L, ex.maxAmountDate.longValue());
    }

    /** 无记录时极值字段均为 null */
    @Test
    public void extremes_emptyRecords() {
        RangeExtremes ex = StatsCalculator.extremes(new ArrayList<>());

        assertNull(ex.maxPrice);
        assertNull(ex.maxPriceDate);
        assertNull(ex.minPrice);
        assertNull(ex.minPriceDate);
        assertNull(ex.maxAmount);
        assertNull(ex.maxAmountDate);
    }

    /** 月度汇总：跨月分组、金额/油量汇总、月内里程极差 */
    @Test
    public void monthly_groupsByMonth() {
        long sep = 1758720000000L;  // 2025-09 下旬（东八区）
        long oct = 1761312000000L;  // 2025-10 下旬（东八区）
        List<RefuelRecord> records = Arrays.asList(
                record(1, sep, 10000, 300.0, 40.0, 7.5, true),
                record(2, sep + 86400000L, 10500, 312.0, 40.0, 7.8, true),
                record(3, oct, 11000, 234.0, 30.0, 7.8, true));
        List<MonthStat> monthly = StatsCalculator.monthly(records);

        assertEquals(2, monthly.size());
        MonthStat first = monthly.get(0);
        assertEquals(612.0, first.amount, 0.0001);
        assertEquals(80.0, first.volume, 0.0001);
        assertEquals(500.0, first.distance, 0.0001);
        MonthStat second = monthly.get(1);
        assertEquals(234.0, second.amount, 0.0001);
        assertEquals(30.0, second.volume, 0.0001);
        assertEquals(0.0, second.distance, 0.0001); // 当月只有一条记录，极差为 0
    }
}
