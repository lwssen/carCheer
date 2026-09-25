package com.carcheer.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.carcheer.app.data.entity.RefuelRecord;

import java.util.List;

@Dao
public interface RecordDao {

    @Insert
    long insert(RefuelRecord record);

    @Update
    void update(RefuelRecord record);

    @Delete
    void delete(RefuelRecord record);

    @Query("SELECT * FROM refuel_records WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    LiveData<List<RefuelRecord>> getByVehicle(long vehicleId);

    /** 时间正序（同刻按 id 正序），供油耗计算使用 */
    @Query("SELECT * FROM refuel_records WHERE vehicleId = :vehicleId ORDER BY date ASC, id ASC")
    List<RefuelRecord> getByVehicleAsc(long vehicleId);

    /** 全部车辆的全部记录，供 CSV 导出 */
    @Query("SELECT * FROM refuel_records ORDER BY vehicleId ASC, date ASC, id ASC")
    List<RefuelRecord> getAllAsc();

    @Query("UPDATE refuel_records SET consumption = :consumption WHERE id = :id")
    void updateConsumption(long id, Double consumption);

    /** 某车辆指定时间（含）之前最近的 N 条记录，按时间倒序，用于油耗计算 */
    @Query("SELECT * FROM refuel_records WHERE vehicleId = :vehicleId AND date <= :beforeDate ORDER BY date DESC, id DESC LIMIT :limit")
    List<RefuelRecord> getRecentBefore(long vehicleId, long beforeDate, int limit);

    /** 查询某车辆在指定时间之前最近的一条加满记录（含该时间），用于油耗结算 */
    @Query("SELECT * FROM refuel_records WHERE vehicleId = :vehicleId AND date <= :beforeDate AND fullTank = 1 ORDER BY date DESC, id DESC LIMIT 1")
    RefuelRecord getLatestFullTankBefore(long vehicleId, long beforeDate);

    @Query("SELECT * FROM refuel_records WHERE id = :id")
    RefuelRecord getById(long id);

    @Query("SELECT * FROM refuel_records WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC LIMIT 1")
    RefuelRecord getLatest(long vehicleId);

    @Query("SELECT COUNT(*) FROM refuel_records WHERE vehicleId = :vehicleId")
    int countByVehicle(long vehicleId);

    /** 动态条件分页查询（vehicleId + 年份区间 + 加满筛选），由 ViewModel 拼 SQL */
    @androidx.room.RawQuery(observedEntities = RefuelRecord.class)
    List<RefuelRecord> queryRecordsPage(androidx.sqlite.db.SupportSQLiteQuery query);

    /** 动态条件 COUNT 查询，配合 queryRecordsPage 用于空态细分 */
    @androidx.room.RawQuery(observedEntities = RefuelRecord.class)
    int countByQuery(androidx.sqlite.db.SupportSQLiteQuery query);

    /** 监听记录表任何增删改，用于分页列表重置刷新 */
    @Query("SELECT COUNT(*) FROM refuel_records")
    LiveData<Integer> observeTableVersion();
}
