package com.carcheer.app.util;

import com.carcheer.app.data.entity.RefuelRecord;
import com.carcheer.app.data.entity.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** CSV 导出/解析，纯 Java 实现，便于单元测试。 */
public final class CsvHelper {

    public static final String[] HEADER = {
            "日期", "车辆", "加油站", "油品", "金额", "油量", "单价", "里程表", "加满", "备注"
    };

    private CsvHelper() {
    }

    /** 导出为 CSV 文本（不含 BOM，写入文件时再补） */
    public static String export(List<RefuelRecord> recordsAsc, Map<Long, Vehicle> vehiclesById) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, HEADER);
        if (recordsAsc != null) {
            for (RefuelRecord r : recordsAsc) {
                Vehicle v = vehiclesById.get(r.vehicleId);
                appendRow(sb, new String[]{
                        DateUtils.format(r.date),
                        v == null ? "" : v.name,
                        r.station == null ? "" : r.station,
                        r.fuelType == null ? "" : r.fuelType,
                        r.amount == null ? "" : trimNumber(r.amount),
                        r.volume == null ? "" : trimNumber(r.volume),
                        r.price == null ? "" : trimNumber(r.price),
                        trimNumber(r.odometer),
                        r.fullTank ? "1" : "0",
                        r.note == null ? "" : r.note
                });
            }
        }
        return sb.toString();
    }

    /** 解析 CSV 文本为行列表，支持带引号字段（内含逗号、引号、换行） */
    public static List<String[]> parse(String csv) {
        List<String[]> rows = new ArrayList<>();
        if (csv == null || csv.isEmpty()) {
            return rows;
        }
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean fieldStarted = false;
        int i = 0;
        while (i < csv.length()) {
            char c = csv.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
                fieldStarted = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
                fieldStarted = true;
            } else if (c == '\r') {
                // 忽略，交给 \n 处理
            } else if (c == '\n') {
                fields.add(field.toString());
                field.setLength(0);
                rows.add(fields.toArray(new String[0]));
                fields.clear();
                fieldStarted = false;
            } else {
                field.append(c);
                fieldStarted = true;
            }
            i++;
        }
        if (field.length() > 0 || fieldStarted || !fields.isEmpty()) {
            fields.add(field.toString());
            rows.add(fields.toArray(new String[0]));
        }
        return rows;
    }

    /** 判断是否为表头行 */
    public static boolean isHeader(String[] row) {
        return row != null && row.length > 0 && HEADER[0].equals(row[0]);
    }

    private static void appendRow(StringBuilder sb, String[] fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields[i]));
        }
        sb.append('\n');
    }

    private static String escape(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")
                || field.contains("\r")) {
            return '"' + field.replace("\"", "\"\"") + '"';
        }
        return field;
    }

    private static String trimNumber(double value) {
        return value == Math.rint(value)
                ? String.valueOf((long) value)
                : String.valueOf(value);
    }
}
