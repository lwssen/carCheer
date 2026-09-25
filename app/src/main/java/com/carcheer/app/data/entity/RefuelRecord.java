package com.carcheer.app.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "refuel_records",
        foreignKeys = @ForeignKey(
                entity = Vehicle.class,
                parentColumns = "id",
                childColumns = "vehicleId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("vehicleId"), @Index({"vehicleId", "odometer"})})
public class RefuelRecord {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long vehicleId;

    /** 加油时间（Unix 毫秒） */
    public long date;

    /** 加油站名称（选填） */
    public String station;

    /** 油品类型，如 92#、95#、0# */
    public String fuelType;

    /** 金额（元），可空 */
    public Double amount;

    /** 油量（升），可空 */
    public Double volume;

    /** 单价（元/升），可空 */
    public Double price;

    /** 里程表读数（km），必填 */
    public double odometer;

    /** 是否加满（参与油耗结算） */
    public boolean fullTank;

    /** 本次计算的百公里油耗，未结算时为 null */
    public Double consumption;

    /** 备注 */
    public String note;
}
