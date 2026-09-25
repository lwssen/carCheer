package com.carcheer.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.carcheer.app.data.entity.RefuelRecord;
import com.carcheer.app.util.FuelCalculator.Result;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FuelCalculatorTest {

    private static RefuelRecord record(long id, double odometer, Double volume, boolean fullTank) {
        RefuelRecord r = new RefuelRecord();
        r.id = id;
        r.vehicleId = 1;
        r.date = id;
        r.odometer = odometer;
        r.volume = volume;
        r.fullTank = fullTank;
        return r;
    }

    /** 两次加满：区间加油 40L、行驶 500km → 油耗 8.00 */
    @Test
    public void twoFullTanks_normal() {
        List<RefuelRecord> records = Arrays.asList(
                record(1, 10000, 40.0, true),
                record(2, 10500, 40.0, true));
        Result result = FuelCalculator.calculate(records);

        assertEquals(1, result.consumptions.size());
        assertEquals(8.0, result.consumptions.get(2L), 0.0001);
        assertEquals(Integer.valueOf(FuelCalculator.ISSUE_FIRST_TANK),
                result.issues.get(1L));
    }

    /** 中间未加满只累计油量：40 + 20 = 60L / 600km → 10.00 */
    @Test
    public void partialTankBetweenFulls_accumulatesVolume() {
        List<RefuelRecord> records = Arrays.asList(
                record(1, 10000, 40.0, true),
                record(2, 10300, 20.0, false),
                record(3, 10600, 40.0, true));
        Result result = FuelCalculator.calculate(records);

        assertEquals(10.0, result.consumptions.get(3L), 0.0001);
        assertNull(result.consumptions.get(2L));
    }

    /** 首条加满记录不结算 */
    @Test
    public void firstFullTank_notSettled() {
        Result result = FuelCalculator.calculate(
                Arrays.asList(record(1, 10000, 40.0, true)));

        assertTrue(result.consumptions.isEmpty());
        assertEquals(Integer.valueOf(FuelCalculator.ISSUE_FIRST_TANK),
                result.issues.get(1L));
    }

    /** 未加满记录本身永不结算（仅首条加满带“首箱”原因） */
    @Test
    public void partialTank_neverSettled() {
        List<RefuelRecord> records = Arrays.asList(
                record(1, 10000, 40.0, true),
                record(2, 10500, 30.0, false),
                record(3, 11000, 30.0, false));
        Result result = FuelCalculator.calculate(records);

        assertTrue(result.consumptions.isEmpty());
        assertEquals(1, result.issues.size());
        assertEquals(Integer.valueOf(FuelCalculator.ISSUE_FIRST_TANK),
                result.issues.get(1L));
    }

    /** 油量缺失的加满记录自身无法结算，但其后的加满记录以它为基准正常结算 */
    @Test
    public void missingVolume_inRange_notSettled() {
        List<RefuelRecord> records = Arrays.asList(
                record(1, 10000, 40.0, true),
                record(2, 10300, null, true),
                record(3, 10600, 40.0, true));
        Result result = FuelCalculator.calculate(records);

        assertEquals(Integer.valueOf(FuelCalculator.ISSUE_VOLUME_MISSING),
                result.issues.get(2L));
        // 记录 2 加满时油箱已满，10300→10600 消耗的即记录 3 的 40L
        assertEquals(13.33, result.consumptions.get(3L), 0.0001);
    }

    /** 里程表倒退 → 无法结算 */
    @Test
    public void odometerBackward_notSettled() {
        List<RefuelRecord> records = Arrays.asList(
                record(1, 10000, 40.0, true),
                record(2, 9900, 40.0, true));
        Result result = FuelCalculator.calculate(records);

        assertEquals(Integer.valueOf(FuelCalculator.ISSUE_ODOMETER_BACKWARD),
                result.issues.get(2L));
    }

    /** 里程无变化 → 无法结算 */
    @Test
    public void zeroDistance_notSettled() {
        List<RefuelRecord> records = Arrays.asList(
                record(1, 10000, 40.0, true),
                record(2, 10000, 40.0, true));
        Result result = FuelCalculator.calculate(records);

        assertEquals(Integer.valueOf(FuelCalculator.ISSUE_ZERO_DISTANCE),
                result.issues.get(2L));
    }

    /** 保留两位小数：38.46L / 500km → 7.69 */
    @Test
    public void rounding_twoDecimals() {
        List<RefuelRecord> records = Arrays.asList(
                record(1, 10000, 38.46, true),
                record(2, 10500, 38.46, true));
        Result result = FuelCalculator.calculate(records);

        assertEquals(7.69, result.consumptions.get(2L), 0.0001);
    }

    /** 一次结算失败不影响后续：下一条加满以最近一次加满（无论其是否结算成功）为基准 */
    @Test
    public void recovery_afterFailedSettlement() {
        List<RefuelRecord> records = Arrays.asList(
                record(1, 10000, 40.0, true),
                record(2, 10300, null, true),   // 缺油量，无法结算
                record(3, 10600, 40.0, true));  // 以记录 2 为基准：40L/300km
        Result result = FuelCalculator.calculate(records);

        assertEquals(Integer.valueOf(FuelCalculator.ISSUE_VOLUME_MISSING),
                result.issues.get(2L));
        assertEquals(13.33, result.consumptions.get(3L), 0.0001);
    }

    /** 空列表与空记录安全 */
    @Test
    public void emptyInput_safe() {
        assertTrue(FuelCalculator.calculate(null).consumptions.isEmpty());
        assertTrue(FuelCalculator.calculate(new ArrayList<>()).issues.isEmpty());
    }
}
