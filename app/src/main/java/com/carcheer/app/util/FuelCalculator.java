package com.carcheer.app.util;

import com.carcheer.app.data.entity.RefuelRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 油耗计算引擎（经典“加满箱”算法），纯 Java 实现，便于单元测试。
 *
 * 规则：对每条“加满”记录，取其上一条“加满”记录：
 *   油耗 = (两条加满记录之间的加油总量，含当前记录) ÷ 区间行驶里程 × 100
 * 未加满的记录只累计油量，不结算油耗。
 */
public final class FuelCalculator {

    /** 无法结算的原因码 */
    public static final int ISSUE_FIRST_TANK = 1;        // 首箱油，无上一次加满参照
    public static final int ISSUE_VOLUME_MISSING = 2;    // 区间内油量缺失
    public static final int ISSUE_ODOMETER_BACKWARD = 3; // 里程表倒退
    public static final int ISSUE_ZERO_DISTANCE = 4;     // 里程无变化

    public static class Result {
        /** recordId -> 百公里油耗（L/100km），仅含已结算记录 */
        public final Map<Long, Double> consumptions = new HashMap<>();
        /** recordId -> 原因码，仅含加满但未能结算的记录 */
        public final Map<Long, Integer> issues = new HashMap<>();
        /** 已结算区间明细（终点记录 id、区间加油量、里程），供加权平均等统计使用 */
        public final List<Segment> segments = new ArrayList<>();
    }

    public static class Segment {
        public final long recordId;
        public final double volumeSum;
        public final double distance;

        Segment(long recordId, double volumeSum, double distance) {
            this.recordId = recordId;
            this.volumeSum = volumeSum;
            this.distance = distance;
        }
    }

    private static class Settlement {
        final Double consumption;
        final int issue;
        final double volumeSum;

        Settlement(Double consumption, int issue, double volumeSum) {
            this.consumption = consumption;
            this.issue = issue;
            this.volumeSum = volumeSum;
        }
    }

    private FuelCalculator() {
    }

    /**
     * @param recordsAsc 同一车辆的加油记录，按时间从早到晚排列
     */
    public static Result calculate(List<RefuelRecord> recordsAsc) {
        Result result = new Result();
        if (recordsAsc == null || recordsAsc.isEmpty()) {
            return result;
        }

        int lastFullIndex = -1;
        for (int i = 0; i < recordsAsc.size(); i++) {
            RefuelRecord record = recordsAsc.get(i);
            if (!record.fullTank) {
                continue;
            }

            if (lastFullIndex < 0) {
                result.issues.put(record.id, ISSUE_FIRST_TANK);
            } else {
                Settlement settlement = settle(recordsAsc, lastFullIndex, i);
                if (settlement.consumption != null) {
                    result.consumptions.put(record.id, settlement.consumption);
                    result.segments.add(new Segment(record.id,
                            settlement.volumeSum,
                            recordsAsc.get(i).odometer - recordsAsc.get(lastFullIndex).odometer));
                } else {
                    result.issues.put(record.id, settlement.issue);
                }
            }
            lastFullIndex = i;
        }
        return result;
    }

    /**
     * 结算 recordsAsc[prev+1 .. cur] 区间的油耗。
     *
     * @return consumption 非空表示结算成功，否则 issue 为原因码
     */
    private static Settlement settle(List<RefuelRecord> recordsAsc, int prev, int cur) {
        double volumeSum = 0;
        for (int k = prev + 1; k <= cur; k++) {
            Double volume = recordsAsc.get(k).volume;
            if (volume == null || volume <= 0) {
                return new Settlement(null, ISSUE_VOLUME_MISSING, 0);
            }
            volumeSum += volume;
        }

        double distance = recordsAsc.get(cur).odometer - recordsAsc.get(prev).odometer;
        if (distance < 0) {
            return new Settlement(null, ISSUE_ODOMETER_BACKWARD, volumeSum);
        }
        if (distance == 0) {
            return new Settlement(null, ISSUE_ZERO_DISTANCE, volumeSum);
        }

        return new Settlement(round(volumeSum / distance * 100, 2), 0, volumeSum);
    }

    private static double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }
}
