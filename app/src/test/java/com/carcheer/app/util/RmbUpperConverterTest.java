package com.carcheer.app.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RmbUpperConverterTest {

    @Test
    public void zero() {
        assertEquals("零元整", RmbUpperConverter.convert(0));
        assertEquals("零元整", RmbUpperConverter.convert(-5));
    }

    @Test
    public void plainInteger() {
        assertEquals("伍仟贰佰伍拾元整", RmbUpperConverter.convert(5250.00));
        assertEquals("壹拾元整", RmbUpperConverter.convert(10.00));
        assertEquals("壹佰壹拾元整", RmbUpperConverter.convert(110.00));
        assertEquals("壹佰万元整", RmbUpperConverter.convert(1000000.00));
        assertEquals("壹亿元整", RmbUpperConverter.convert(100000000.00));
    }

    @Test
    public void zerosInsideNumber() {
        assertEquals("壹万零伍拾元整", RmbUpperConverter.convert(10050.00));
        assertEquals("壹万零伍佰元整", RmbUpperConverter.convert(10500.00));
        assertEquals("壹亿零伍万元整", RmbUpperConverter.convert(100050000.00));
        assertEquals("壹拾万零伍元整", RmbUpperConverter.convert(100005.00));
        assertEquals("壹万贰仟叁佰肆拾伍元陆角柒分", RmbUpperConverter.convert(12345.67));
    }

    @Test
    public void jiaoAndFen() {
        assertEquals("壹万零伍拾元伍角", RmbUpperConverter.convert(10050.50));
        assertEquals("壹万零伍佰元零伍分", RmbUpperConverter.convert(10500.05));
        assertEquals("伍角叁分", RmbUpperConverter.convert(0.53));
        assertEquals("叁分", RmbUpperConverter.convert(0.03));
        assertEquals("贰角", RmbUpperConverter.convert(0.20));
    }

    @Test
    public void rounding() {
        // 1.005 → 四舍五入到 101 分 → 壹元零壹分
        assertEquals("壹元零壹分", RmbUpperConverter.convert(1.005));
    }
}
