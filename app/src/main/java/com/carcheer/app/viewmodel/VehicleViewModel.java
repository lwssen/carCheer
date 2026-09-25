package com.carcheer.app.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.carcheer.app.data.AppDatabase;
import com.carcheer.app.data.dao.VehicleDao;
import com.carcheer.app.data.entity.Vehicle;
import com.carcheer.app.data.pojo.VehicleRecordCount;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VehicleViewModel extends AndroidViewModel {

    private final VehicleDao vehicleDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public VehicleViewModel(Application application) {
        super(application);
        vehicleDao = AppDatabase.getInstance(application).vehicleDao();
    }

    public LiveData<List<Vehicle>> getVehicles() {
        return vehicleDao.getAll();
    }

    public LiveData<List<VehicleRecordCount>> getRecordCounts() {
        return vehicleDao.countAllRecords();
    }

    public void save(Vehicle vehicle) {
        executor.execute(() -> {
            // 设为默认时先清除其他车辆的默认标识，并重置手动选择记忆
            if (vehicle.isDefault) {
                vehicleDao.clearDefault();
                com.carcheer.app.util.SelectedVehiclePrefs.clear(getApplication());
            }
            if (vehicle.id == 0) {
                // 新建车辆：未填当前里程时以初始里程作为当前里程
                if (vehicle.currentOdometer == 0) {
                    vehicle.currentOdometer = vehicle.initialOdometer;
                }
                vehicleDao.insert(vehicle);
            } else {
                vehicleDao.update(vehicle);
            }
        });
    }

    public void delete(Vehicle vehicle) {
        executor.execute(() -> vehicleDao.delete(vehicle));
    }

    /** 清空全部数据（车辆与加油记录），回调在主线程执行 */
    public void clearAll(Runnable onDone) {
        executor.execute(() -> {
            vehicleDao.clearAll();
            if (onDone != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(onDone);
            }
        });
    }

    /** 仅用于编辑前回填表单的单次查询 */
    public Vehicle getVehicleSync(long id) {
        try {
            return executor.submit(() -> vehicleDao.getById(id)).get();
        } catch (Exception e) {
            return null;
        }
    }
}
