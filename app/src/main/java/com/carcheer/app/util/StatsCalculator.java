package com.carcheer.app.util;

import com.carcheer.app.data.entity.RefuelRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 统计计算，纯 Java 实现，便于单元测试。输入均为某车辆时间正序的记录。 */
public final class StatsCalculator {

    private static final SimpleDateFormat MONTH_FORMAT =
            new SimpleDateFormat("yyyy-MM", Locale.CHINA);

    public static class Overview {
        public int count;
        public double totalAmount;
        public double totalVolume;
        /** 已结算区间的加权平均油耗（总油量/总里程×100），无法计算时为 null */
        public Double avgConsumption;
        /** 最新一次有单价的记录的单价 */
        public Double latestPrice;
        /** 上一次有单价的记录的单价 */
        public Double previousPrice;
    }

    public static class MonthStat {
        /** 如 2026-09 */
        public final String month;
        public double amount;
        public double volume;
        /** 当月内里程表最大最小值之差，非当月行驶总里程的精确值 */
        public double distance;

        MonthStat(String month) {
            this.month = month;
        }
    }

    private StatsCalculator() {
    }

    public static Overview overview(List<RefuelRecord> recordsAsc, FuelCalculator.Result fuelResult) {
        Overview ov = new Overview();
        if (recordsAsc == null) {
            return ov;
        }
        ov.count = recordsAsc.size();

        Double latest = null;
        Double previous = null;
        for (RefuelRecord r : recordsAsc) {
            if (r.amount != null) {
                ov.totalAmount += r.amount;
            }
            if (r.volume != null) {
                ov.totalVolume += r.volume;
            }
            if (r.price != null) {
                previous = latest;
                latest = r.price;
            }
        }
        ov.latestPrice = latest;
        ov.previousPrice = previous;

        if (fuelResult != null && !fuelResult.segments.isEmpty()) {
            double volumeSum = 0;
            double distance = 0;
            for (FuelCalculator.Segment s : fuelResult.segments) {
                volumeSum += s.volumeSum;
                distance += s.distance;
            }
            if (distance > 0) {
                ov.avgConsumption = round(volumeSum / distance * 100, 2);
            }
        }
        return ov;
    }

    /** 时间段内的最高油价/最高加油金额及其发生日期（并列时取最近一次） */
    public static class RangeExtremes {
        public Double maxPrice;
        public Long maxPriceDate;
        public Double maxAmount;
        public Long maxAmountDate;
    }

    public static RangeExtremes extremes(List<RefuelRecord> recordsAsc) {
        RangeExtremes ex = new RangeExtremes();
        if (recordsAsc == null) {
            return ex;
        }
        for (RefuelRecord r : recordsAsc) {
            if (r.price != null && (ex.maxPrice == null || r.price >= ex.maxPrice)) {
                ex.maxPrice = r.price;
                ex.maxPriceDate = r.date;
            }
            if (r.amount != null && (ex.maxAmount == null || r.amount >= ex.maxAmount)) {
                ex.maxAmount = r.amount;
                ex.maxAmountDate = r.date;
            }
        }
        return ex;
    }

    /** 按月汇总，返回按时间正序排列的列表 */
    public static List<MonthStat> monthly(List<RefuelRecord> recordsAsc) {
        Map<String, MonthStat> byMonth = new LinkedHashMap<>();
        if (recordsAsc == null) {
            return new ArrayList<>();
        }
        for (RefuelRecord r : recordsAsc) {
            String month = MONTH_FORMAT.format(new Date(r.date));
            MonthStat stat = byMonth.get(month);
            if (stat == null) {
                stat = new MonthStat(month);
                byMonth.put(month, stat);
            }
            if (r.amount != null) {
                stat.amount += r.amount;
            }
            if (r.volume != null) {
                stat.volume += r.volume;
            }
            if (stat.distance == 0) {
                stat.distance = r.odometer;
            } else {
                stat.distance = Math.max(stat.distance, r.odometer);
            }
        }
        // distance 暂存的是最大里程，再减去每月最小里程
        Map<String, Double> minOdo = new LinkedHashMap<>();
        for (RefuelRecord r : recordsAsc) {
            String month = MONTH_FORMAT.format(new Date(r.date));
            Double min = minOdo.get(month);
            minOdo.put(month, min == null ? r.odometer : Math.min(min, r.odometer));
        }
        List<MonthStat> result = new ArrayList<>(byMonth.values());
        for (MonthStat stat : result) {
            Double min = minOdo.get(stat.month);
            stat.distance = Math.max(0, stat.distance - (min == null ? 0 : min));
        }
        return result;
    }

    private static double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }
}
