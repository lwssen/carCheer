package com.carcheer.app.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.carcheer.app.data.AppDatabase;
import com.carcheer.app.data.dao.RecordDao;
import com.carcheer.app.data.dao.VehicleDao;
import com.carcheer.app.data.entity.RefuelRecord;
import com.carcheer.app.data.entity.Vehicle;
import com.carcheer.app.util.FuelCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordViewModel extends AndroidViewModel {

    public static final int PAGE_SIZE = 6;

    private final RecordDao recordDao;
    private final VehicleDao vehicleDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Long> vehicleId = new MutableLiveData<>();

    /** 筛选条件：年份 0 表示全部年份；加满 0=全部 1=未加满 2=加满 */
    private int yearFilter = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
    private int fullTankFilter;

    private final List<RefuelRecord> pagedAccumulated = new ArrayList<>();
    private final MutableLiveData<List<RefuelRecord>> pagedRecords = new MutableLiveData<>();
    private final LiveData<Integer> tableVersion;
    private int nextPage;
    private boolean loadingPage;
    private boolean hasMore = true;
    private int pagingGeneration;

    public RecordViewModel(Application application) {
        super(application);
        recordDao = AppDatabase.getInstance(application).recordDao();
        vehicleDao = AppDatabase.getInstance(application).vehicleDao();
        tableVersion = recordDao.observeTableVersion();
    }

    public void setVehicleId(long id) {
        Long current = vehicleId.getValue();
        if (id != 0 && (current == null || current != id)) {
            vehicleId.setValue(id);
            resetPaging();
        }
    }

    public LiveData<Long> getVehicleId() {
        return vehicleId;
    }

    public LiveData<List<RefuelRecord>> getRecords() {
        return Transformations.switchMap(vehicleId, recordDao::getByVehicle);
    }

    public LiveData<List<Vehicle>> getVehicles() {
        return vehicleDao.getAll();
    }

    /** 分页累加的记录列表（按当前筛选条件） */
    public LiveData<List<RefuelRecord>> getPagedRecords() {
        return pagedRecords;
    }

    /** 记录表版本号（任何增删改都会变化），UI 层观察到后调用 resetPaging() */
    public LiveData<Integer> getTableVersion() {
        return tableVersion;
    }

    public void setYearFilter(int year) {
        if (yearFilter != year) {
            yearFilter = year;
            resetPaging();
        }
    }

    public int getYearFilter() {
        return yearFilter;
    }

    public int getFullTankFilter() {
        return fullTankFilter;
    }

    public void setFullTankFilter(int filter) {
        if (fullTankFilter != filter) {
            fullTankFilter = filter;
            resetPaging();
        }
    }

    /** 清空已加载数据并从第一页重新查询 */
    public void resetPaging() {
        pagingGeneration++;
        nextPage = 0;
        hasMore = true;
        loadingPage = false;
        pagedAccumulated.clear();
        pagedRecords.setValue(new ArrayList<>(pagedAccumulated));
        loadNextPage();
    }

    /** 触底加载下一页，结果追加到已加载集合 */
    public void loadNextPage() {
        if (loadingPage || !hasMore) {
            return;
        }
        Long vid = vehicleId.getValue();
        if (vid == null || vid == 0) {
            return;
        }
        loadingPage = true;
        final int gen = pagingGeneration;
        final int offset = nextPage * PAGE_SIZE;
        executor.execute(() -> {
            List<RefuelRecord> pageData = recordDao.queryRecordsPage(buildPageQuery(vid, offset));
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                // 期间发生过重置（切换车辆/筛选/数据变更），丢弃过期页
                if (gen != pagingGeneration) {
                    return;
                }
                pagedAccumulated.addAll(pageData);
                nextPage++;
                hasMore = pageData.size() == PAGE_SIZE;
                loadingPage = false;
                pagedRecords.setValue(new ArrayList<>(pagedAccumulated));
            });
        });
    }

    private androidx.sqlite.db.SimpleSQLiteQuery buildPageQuery(long vid, int offset) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM refuel_records WHERE vehicleId = ?");
        args.add(vid);
        if (yearFilter != 0) {
            sql.append(" AND date >= ? AND date < ?");
            args.add(yearStartMillis(yearFilter));
            args.add(yearStartMillis(yearFilter + 1));
        }
        if (fullTankFilter == 1) {
            sql.append(" AND fullTank = 0");
        } else if (fullTankFilter == 2) {
            sql.append(" AND fullTank = 1");
        }
        sql.append(" ORDER BY date DESC, id DESC LIMIT ? OFFSET ?");
        args.add(PAGE_SIZE);
        args.add(offset);
        return new androidx.sqlite.db.SimpleSQLiteQuery(sql.toString(), args.toArray());
    }

    private static long yearStartMillis(int year) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(year, java.util.Calendar.JANUARY, 1, 0, 0, 0);
        return c.getTimeInMillis();
    }

    public void save(RefuelRecord record) {
        executor.execute(() -> {
            if (record.id == 0) {
                record.id = recordDao.insert(record);
            } else {
                recordDao.update(record);
            }
            recalculateConsumption(record.vehicleId);
            updateVehicleOdometer(record);
        });
    }

    /** 若保存的是该车最新日期的记录，把里程同步到车辆表：当前里程=本次输入，上次里程=更新前的当前里程 */
    private void updateVehicleOdometer(RefuelRecord record) {
        if (record.odometer <= 0) {
            return;
        }
        RefuelRecord latest = recordDao.getLatest(record.vehicleId);
        if (latest == null || latest.id != record.id) {
            return;
        }
        Vehicle vehicle = vehicleDao.getById(record.vehicleId);
        if (vehicle == null) {
            return;
        }
        vehicle.lastOdometer = vehicle.currentOdometer;
        vehicle.currentOdometer = record.odometer;
        vehicleDao.update(vehicle);
    }

    public void delete(RefuelRecord record) {
        executor.execute(() -> {
            recordDao.delete(record);
            recalculateConsumption(record.vehicleId);
        });
    }

    /** 记录变动后全量重算该车辆各记录的油耗（数据量小，简单可靠） */
    private void recalculateConsumption(long vehicleId) {
        List<RefuelRecord> asc = recordDao.getByVehicleAsc(vehicleId);
        FuelCalculator.Result result = FuelCalculator.calculate(asc);
        for (RefuelRecord record : asc) {
            Double consumption = result.consumptions.get(record.id);
            if (consumption != null || result.issues.containsKey(record.id)) {
                if (!equalsNullSafe(record.consumption, consumption)) {
                    recordDao.updateConsumption(record.id, consumption);
                }
            }
        }
    }

    private static boolean equalsNullSafe(Double a, Double b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.doubleValue() == b.doubleValue();
    }

    public RefuelRecord getByIdSync(long id) {
        try {
            return executor.submit(() -> recordDao.getById(id)).get();
        } catch (Exception e) {
            return null;
        }
    }

    public Vehicle getVehicleSync(long id) {
        try {
            return executor.submit(() -> vehicleDao.getById(id)).get();
        } catch (Exception e) {
            return null;
        }
    }

    /** 里程预填值：上一条记录的里程表读数，没有记录则用车辆初始里程 */
    public double getPrefillOdometerSync(long vehicleId, Vehicle vehicle) {
        RefuelRecord latest = getByIdSyncOnly(vehicleId);
        if (latest != null) {
            return latest.odometer;
        }
        return vehicle != null ? vehicle.initialOdometer : 0;
    }

    public interface ImportCallback {
        void onResult(int insertedRecords, int createdVehicles);
    }

    /** 导出 CSV 文本（后台线程执行，回调在主线程） */
    public void exportCsv(java.util.function.Consumer<String> onDone) {
        executor.execute(() -> {
            List<RefuelRecord> records = recordDao.getAllAsc();
            java.util.Map<Long, Vehicle> vehiclesById = new java.util.HashMap<>();
            for (Vehicle v : vehicleDao.getAllSync()) {
                vehiclesById.put(v.id, v);
            }
            String csv = com.carcheer.app.util.CsvHelper.export(records, vehiclesById);
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> onDone.accept(csv));
        });
    }

    /** 导入 CSV：按车辆名匹配（无则创建），逐行插入后逐车重算油耗 */
    public void importCsv(String csvText, ImportCallback callback) {
        executor.execute(() -> {
            List<String[]> rows = com.carcheer.app.util.CsvHelper.parse(csvText);
            int inserted = 0;
            int createdVehicles = 0;
            java.util.Set<Long> touchedVehicles = new java.util.HashSet<>();
            for (String[] row : rows) {
                if (com.carcheer.app.util.CsvHelper.isHeader(row) || row.length < HEADER_COLUMNS) {
                    continue;
                }
                long date = com.carcheer.app.util.DateUtils.parse(row[0]);
                if (date == 0) {
                    continue;
                }
                String vehicleName = row[1].trim();
                if (vehicleName.isEmpty()) {
                    continue;
                }
                Vehicle vehicle = vehicleDao.getByName(vehicleName);
                if (vehicle == null) {
                    vehicle = new Vehicle();
                    vehicle.name = vehicleName;
                    vehicle.id = vehicleDao.insert(vehicle);
                    createdVehicles++;
                }
                RefuelRecord record = new RefuelRecord();
                record.vehicleId = vehicle.id;
                record.date = date;
                record.station = blankToNull(row[2]);
                record.fuelType = blankToNull(row[3]);
                record.amount = parseOrNull(row[4]);
                record.volume = parseOrNull(row[5]);
                record.price = parseOrNull(row[6]);
                Double odometer = parseOrNull(row[7]);
                record.odometer = odometer == null ? 0 : odometer;
                record.fullTank = "1".equals(row[8].trim());
                record.note = blankToNull(row[9]);
                recordDao.insert(record);
                inserted++;
                touchedVehicles.add(vehicle.id);
            }
            for (Long vehicleId : touchedVehicles) {
                recalculateConsumption(vehicleId);
            }
            final int fInserted = inserted;
            final int fCreated = createdVehicles;
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> callback.onResult(fInserted, fCreated));
        });
    }

    private static final int HEADER_COLUMNS = 10;

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static Double parseOrNull(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            double v = Double.parseDouble(s.trim());
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private RefuelRecord getByIdSyncOnly(long vid) {
        try {
            return executor.submit(() -> recordDao.getLatest(vid)).get();
        } catch (Exception e) {
            return null;
        }
    }
}
