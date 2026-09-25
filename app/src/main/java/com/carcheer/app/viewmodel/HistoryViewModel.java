package com.carcheer.app.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.carcheer.app.data.AppDatabase;
import com.carcheer.app.data.dao.RecordDao;
import com.carcheer.app.data.entity.RefuelRecord;
import com.carcheer.app.util.HistoryRangeCalculator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryViewModel extends AndroidViewModel {

    public static final int PAGE_SIZE = 6;

    public static final int DATA_TYPE_ALL = 0;
    public static final int DATA_TYPE_MAX_PRICE = 1;
    public static final int DATA_TYPE_MAX_AMOUNT = 2;

    private final RecordDao recordDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Long> vehicleId = new MutableLiveData<>();
    private final MutableLiveData<Long> startLive = new MutableLiveData<>();
    private final MutableLiveData<Long> endLive = new MutableLiveData<>();
    private final List<RefuelRecord> pagedAccumulated = new ArrayList<>();
    private final MutableLiveData<List<RefuelRecord>> pagedRecords = new MutableLiveData<>();
    /** 0=无提示 1=时间段内无记录 2=时间段有记录但无有效油价/金额数据 */
    private final MutableLiveData<Integer> emptyHint = new MutableLiveData<>(0);
    /** 当前查询条件全量数据的汇总（统计卡片用） */
    private final MutableLiveData<com.carcheer.app.util.StatsCalculator.Overview> summaryLive =
            new MutableLiveData<>();
    private final MutableLiveData<com.carcheer.app.util.StatsCalculator.RangeExtremes> extremesLive =
            new MutableLiveData<>();
    private final LiveData<Integer> tableVersion;

    private int dataType = DATA_TYPE_ALL;
    private int nextPage;
    private boolean loadingPage;
    private boolean hasMore = true;
    private int pagingGeneration;

    public HistoryViewModel(Application application) {
        super(application);
        recordDao = AppDatabase.getInstance(application).recordDao();
        tableVersion = recordDao.observeTableVersion();
    }

    public LiveData<Integer> getTableVersion() {
        return tableVersion;
    }

    public LiveData<List<RefuelRecord>> getPagedRecords() {
        return pagedRecords;
    }

    public LiveData<Integer> getEmptyHint() {
        return emptyHint;
    }

    public LiveData<com.carcheer.app.util.StatsCalculator.Overview> getSummary() {
        return summaryLive;
    }

    public LiveData<com.carcheer.app.util.StatsCalculator.RangeExtremes> getExtremes() {
        return extremesLive;
    }

    public int getDataType() {
        return dataType;
    }

    public LiveData<Long> getStartLive() {
        return startLive;
    }

    public LiveData<Long> getEndLive() {
        return endLive;
    }

    /** 与记录页的车辆选择联动 */
    public void setVehicleId(long id) {
        Long current = vehicleId.getValue();
        if (id != 0 && (current == null || current != id)) {
            vehicleId.setValue(id);
            resetPaging();
        }
    }

    /** 时间类型下拉：预设类型立即反算区间，自定义等待用户选日期 */
    public void setTimeType(int type) {
        if (type == HistoryRangeCalculator.TYPE_CUSTOM) {
            return;
        }
        long now = System.currentTimeMillis();
        long[] range = HistoryRangeCalculator.range(type, now);
        startLive.setValue(range[0]);
        endLive.setValue(range[1]);
        resetPaging();
    }

    /** 自定义起止日期（日期选择器回调，主线程） */
    public void setCustomRange(long startMillis, long endMillis) {
        if (endMillis < startMillis) {
            long tmp = startMillis;
            startMillis = endMillis;
            endMillis = tmp;
        }
        startLive.setValue(startMillis);
        endLive.setValue(endMillis);
        resetPaging();
    }

    public void setDataType(int type) {
        if (dataType != type) {
            dataType = type;
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
        emptyHint.setValue(0);
        loadNextPage();
        refreshSummary();
    }

    /** 按当前查询条件的全量数据计算汇总（统计卡片），与分页加载互不影响 */
    private void refreshSummary() {
        final int gen = pagingGeneration;
        Long vid = vehicleId.getValue();
        Long start = startLive.getValue();
        Long end = endLive.getValue();
        if (vid == null || vid == 0 || start == null || end == null) {
            summaryLive.setValue(null);
            return;
        }
        executor.execute(() -> {
            List<RefuelRecord> desc = recordDao.queryRecordsPage(buildFullQuery(vid, start, end));
            List<RefuelRecord> asc = new ArrayList<>(desc);
            java.util.Collections.reverse(asc);
            com.carcheer.app.util.FuelCalculator.Result fuel =
                    com.carcheer.app.util.FuelCalculator.calculate(asc);
            com.carcheer.app.util.StatsCalculator.Overview ov =
                    com.carcheer.app.util.StatsCalculator.overview(asc, fuel);
            com.carcheer.app.util.StatsCalculator.RangeExtremes ex =
                    com.carcheer.app.util.StatsCalculator.extremes(asc);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (gen == pagingGeneration) {
                    summaryLive.setValue(ov);
                    extremesLive.setValue(ex);
                }
            });
        });
    }

    /** 触底加载下一页，结果追加到已加载集合 */
    public void loadNextPage() {
        if (loadingPage || !hasMore) {
            return;
        }
        Long vid = vehicleId.getValue();
        Long start = startLive.getValue();
        Long end = endLive.getValue();
        if (vid == null || vid == 0 || start == null || end == null) {
            return;
        }
        loadingPage = true;
        final int gen = pagingGeneration;
        final int offset = nextPage * PAGE_SIZE;
        executor.execute(() -> {
            List<RefuelRecord> pageData = recordDao.queryRecordsPage(buildPageQuery(vid, start, end, offset));
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
                if (pagedAccumulated.isEmpty() && !hasMore) {
                    resolveEmptyHint(vid, start, end);
                }
            });
        });
    }

    /** 最高油价/最高金额模式下，区分"时间段无记录"与"有记录但字段无效" */
    private void resolveEmptyHint(long vid, long start, long end) {
        if (dataType == DATA_TYPE_ALL) {
            emptyHint.setValue(1);
            return;
        }
        executor.execute(() -> {
            int count = recordDao.countByQuery(buildCountQuery(vid, start, end));
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> emptyHint.setValue(count > 0 ? 2 : 1));
        });
    }

    private androidx.sqlite.db.SimpleSQLiteQuery buildPageQuery(long vid, long start, long end, int offset) {
        List<Object> args = new ArrayList<>();
        String sql = buildWhere(args, vid, start, end);
        args.add(PAGE_SIZE);
        args.add(offset);
        return new androidx.sqlite.db.SimpleSQLiteQuery(sql + " LIMIT ? OFFSET ?", args.toArray());
    }

    private androidx.sqlite.db.SimpleSQLiteQuery buildCountQuery(long vid, long start, long end) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM refuel_records WHERE " + baseWhere(args, vid, start, end);
        return new androidx.sqlite.db.SimpleSQLiteQuery(sql, args.toArray());
    }

    private androidx.sqlite.db.SimpleSQLiteQuery buildFullQuery(long vid, long start, long end) {
        List<Object> args = new ArrayList<>();
        String sql = buildWhere(args, vid, start, end);
        return new androidx.sqlite.db.SimpleSQLiteQuery(sql, args.toArray());
    }

    private String buildWhere(List<Object> args, long vid, long start, long end) {
        String base = baseWhere(args, vid, start, end);
        String sql;
        if (dataType == DATA_TYPE_MAX_PRICE) {
            args.addAll(Arrays.asList(vid, start, end));
            sql = "SELECT * FROM refuel_records WHERE " + base
                    + " AND price IS NOT NULL AND price = (SELECT MAX(price) FROM refuel_records WHERE "
                    + base + " AND price IS NOT NULL)";
        } else if (dataType == DATA_TYPE_MAX_AMOUNT) {
            args.addAll(Arrays.asList(vid, start, end));
            sql = "SELECT * FROM refuel_records WHERE " + base
                    + " AND amount IS NOT NULL AND amount = (SELECT MAX(amount) FROM refuel_records WHERE "
                    + base + " AND amount IS NOT NULL)";
        } else {
            sql = "SELECT * FROM refuel_records WHERE " + base;
        }
        return sql + " ORDER BY date DESC, id DESC";
    }

    private String baseWhere(List<Object> args, long vid, long start, long end) {
        args.add(vid);
        args.add(start);
        args.add(end);
        return "vehicleId = ? AND date >= ? AND date <= ?";
    }
}
