package com.carcheer.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.carcheer.app.data.entity.RefuelRecord;
import com.carcheer.app.data.entity.Vehicle;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvHelperTest {

    /** 导出包含表头，且数值/布尔/文本列齐全 */
    @Test
    public void export_containsHeaderAndRows() {
        Vehicle v = new Vehicle();
        v.id = 1;
        v.name = "我的小白";

        RefuelRecord r = new RefuelRecord();
        r.id = 1;
        r.vehicleId = 1;
        r.date = DateUtils.parse("2026-09-25 08:30");
        r.station = "中石化";
        r.fuelType = "92#";
        r.amount = 300.0;
        r.volume = 38.46;
        r.price = 7.8;
        r.odometer = 10000.0;
        r.fullTank = true;
        r.note = null;

        String csv = CsvHelper.export(Collections.singletonList(r),
                Collections.singletonMap(1L, v));
        String[] lines = csv.split("\n");

        assertEquals(2, lines.length);
        assertTrue(lines[0].startsWith(CsvHelper.HEADER[0]));
        String[] row = CsvHelper.parse(csv).get(1);
        assertEquals("2026-09-25 08:30", row[0]);
        assertEquals("我的小白", row[1]);
        assertEquals("中石化", row[2]);
        assertEquals("92#", row[3]);
        assertEquals("300", row[4]);
        assertEquals("38.46", row[5]);
        assertEquals("7.8", row[6]);
        assertEquals("10000", row[7]);
        assertEquals("1", row[8]);
        assertEquals("", row[9]);
    }

    /** 含逗号、引号、换行的字段正确转义且可解析还原 */
    @Test
    public void roundTrip_specialCharacters() {
        String csv = "2026-09-25 08:30,车A,\"站,名\",\"含\"\"引\"\"号\",300,38.46,7.8,10000,1,\"多\n行\"";

        List<String[]> rows = CsvHelper.parse(csv);
        assertEquals(1, rows.size());
        String[] row = rows.get(0);
        assertEquals("站,名", row[2]);
        assertEquals("含\"引\"号", row[3]);
        assertEquals("多\n行", row[9]);
    }

    /** 多行解析与行数 */
    @Test
    public void parse_multipleLines() {
        List<String[]> rows = CsvHelper.parse("a,b,c\n1,2,3\n4,5,6");
        assertEquals(3, rows.size());
        assertEquals("5", rows.get(2)[1]);
    }

    /** 表头行识别 */
    @Test
    public void isHeader_detectsHeader() {
        assertTrue(CsvHelper.isHeader(CsvHelper.HEADER));
        assertFalse(CsvHelper.isHeader(new String[]{"2026-09-25 08:30", "车A"}));
        assertFalse(CsvHelper.isHeader(null));
    }

    /** 导出→解析 round trip 数据无损 */
    @Test
    public void exportThenParse_roundTrip() {
        Vehicle v = new Vehicle();
        v.id = 2;
        v.name = "车,B";  // 名字带逗号

        RefuelRecord r = new RefuelRecord();
        r.id = 1;
        r.vehicleId = 2;
        r.date = DateUtils.parse("2026-01-01 12:00");
        r.fuelType = "0# 柴油";
        r.amount = 100.0;
        r.volume = 12.5;
        r.price = 8.0;
        r.odometer = 5000.0;
        r.fullTank = false;
        r.note = "备注\"x\"";

        Map<Long, Vehicle> vehicles = new HashMap<>();
        vehicles.put(2L, v);
        List<String[]> rows = CsvHelper.parse(
                CsvHelper.export(Collections.singletonList(r), vehicles));

        assertEquals(2, rows.size());
        String[] row = rows.get(1);
        assertEquals("车,B", row[1]);
        assertEquals("备注\"x\"", row[9]);
        assertEquals("0", row[8]);
        assertEquals("0# 柴油", row[3]);
    }
}
