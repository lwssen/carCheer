package com.carcheer.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.carcheer.app.data.entity.Vehicle;
import com.carcheer.app.data.pojo.VehicleRecordCount;

import java.util.List;

@Dao
public interface VehicleDao {

    @Insert
    long insert(Vehicle vehicle);

    @Update
    void update(Vehicle vehicle);

    @Delete
    void delete(Vehicle vehicle);

    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    LiveData<List<Vehicle>> getAll();

    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    List<Vehicle> getAllSync();

    @Query("SELECT * FROM vehicles WHERE id = :id")
    Vehicle getById(long id);

    @Query("SELECT * FROM vehicles WHERE name = :name LIMIT 1")
    Vehicle getByName(String name);

    /** 清空全部车辆（记录随外键级联删除） */
    @Query("DELETE FROM vehicles")
    void clearAll();

    @Query("UPDATE vehicles SET isDefault = 0")
    void clearDefault();

    @Query("SELECT * FROM vehicles WHERE isDefault = 1 LIMIT 1")
    Vehicle getDefault();

    @Query("SELECT vehicleId AS vehicleId, COUNT(*) AS recordCount FROM refuel_records GROUP BY vehicleId")
    LiveData<List<VehicleRecordCount>> countAllRecords();
}
