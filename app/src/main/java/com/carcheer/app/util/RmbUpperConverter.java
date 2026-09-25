package com.carcheer.app.util;

import java.util.ArrayList;
import java.util.List;

/** 人民币金额转中文大写（如 5250.00 → 伍仟贰佰伍拾元整），纯 Java 便于单元测试 */
public final class RmbUpperConverter {

    private static final char[] DIGITS = {'零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'};
    private static final String[] UNIT_DIGIT = {"", "拾", "佰", "仟"};
    private static final String[] UNIT_GROUP = {"", "万", "亿", "万亿"};

    private RmbUpperConverter() {
    }

    public static String convert(double amount) {
        // BigDecimal 四舍五入，规避二进制浮点精度（如 1.005 实为 1.00499...）
        long cents = java.math.BigDecimal.valueOf(amount)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
        if (cents <= 0) {
            return "零元整";
        }
        long yuan = cents / 100;
        int jiao = (int) (cents % 100) / 10;
        int fen = (int) (cents % 10);

        StringBuilder sb = new StringBuilder();
        sb.append(convertInteger(yuan));
        if (yuan > 0) {
            sb.append('元');
        }
        if (jiao == 0 && fen == 0) {
            if (yuan > 0) {
                sb.append('整');
            }
        } else {
            if (jiao == 0 && fen > 0 && yuan > 0) {
                sb.append('零');
            }
            if (jiao > 0) {
                sb.append(DIGITS[jiao]).append('角');
            }
            if (fen > 0) {
                sb.append(DIGITS[fen]).append('分');
            }
        }
        return sb.toString();
    }

    /** 整数部分按 4 位一组转换，组间补零（如 100050000 → 壹亿零伍万） */
    private static String convertInteger(long n) {
        List<Integer> groups = new ArrayList<>();
        while (n > 0) {
            groups.add((int) (n % 10000));
            n /= 10000;
        }
        StringBuilder sb = new StringBuilder();
        boolean needZero = false;
        for (int i = groups.size() - 1; i >= 0; i--) {
            int group = groups.get(i);
            if (group == 0) {
                if (sb.length() > 0) {
                    needZero = true;
                }
                continue;
            }
            if (needZero || (sb.length() > 0 && group < 1000)) {
                sb.append('零');
            }
            needZero = false;
            sb.append(convertGroup(group)).append(UNIT_GROUP[i]);
        }
        return sb.toString();
    }

    private static String convertGroup(int group) {
        StringBuilder sb = new StringBuilder();
        boolean zeroPending = false;
        for (int pos = 3; pos >= 0; pos--) {
            int digit = group / pow10(pos) % 10;
            if (digit == 0) {
                if (sb.length() > 0) {
                    zeroPending = true;
                }
            } else {
                if (zeroPending) {
                    sb.append('零');
                    zeroPending = false;
                }
                sb.append(DIGITS[digit]).append(UNIT_DIGIT[pos]);
            }
        }
        return sb.toString();
    }

    private static int pow10(int exponent) {
        int result = 1;
        for (int i = 0; i < exponent; i++) {
            result *= 10;
        }
        return result;
    }
}
