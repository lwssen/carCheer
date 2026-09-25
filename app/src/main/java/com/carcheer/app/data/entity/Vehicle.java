package com.carcheer.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vehicles")
public class Vehicle {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** 车辆名称（必填） */
    @NonNull
    public String name = "";

    /** 车牌号（选填） */
    public String plateNo;

    /** 初始里程表读数（km） */
    public double initialOdometer;

    /** 油箱容积（L），0 表示未填写 */
    public double tankCapacity;

    /** 当前里程：该车最新一条加油记录的里程表读数 */
    public double currentOdometer;

    /** 上次里程：当前里程更新前的值 */
    public double lastOdometer;

    /** 是否为默认车辆（记录/统计页初始选中） */
    public boolean isDefault;
}
