package com.carcheer.app.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.carcheer.app.data.dao.RecordDao;
import com.carcheer.app.data.dao.VehicleDao;
import com.carcheer.app.data.entity.RefuelRecord;
import com.carcheer.app.data.entity.Vehicle;

@Database(
        entities = {Vehicle.class, RefuelRecord.class},
        version = 3,
        exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    /** v2：车辆表增加默认标识列 */
    static final androidx.room.migration.Migration MIGRATION_1_2 =
            new androidx.room.migration.Migration(1, 2) {
                @Override
                public void migrate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
                    db.execSQL("ALTER TABLE vehicles ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0");
                }
            };

    /** v3：车辆表增加当前里程/上次里程列，并用存量加油记录回填 */
    static final androidx.room.migration.Migration MIGRATION_2_3 =
            new androidx.room.migration.Migration(2, 3) {
                @Override
                public void migrate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
                    db.execSQL("ALTER TABLE vehicles ADD COLUMN currentOdometer REAL NOT NULL DEFAULT 0");
                    db.execSQL("ALTER TABLE vehicles ADD COLUMN lastOdometer REAL NOT NULL DEFAULT 0");
                    // 当前里程 = 该车最新一条记录的里程表；上次里程 = 次新一条的里程表；无记录的车保持 0
                    db.execSQL("UPDATE vehicles SET currentOdometer = IFNULL(("
                            + "SELECT r.odometer FROM refuel_records r WHERE r.vehicleId = vehicles.id "
                            + "ORDER BY r.date DESC, r.id DESC LIMIT 1), 0)");
                    db.execSQL("UPDATE vehicles SET lastOdometer = IFNULL(("
                            + "SELECT r.odometer FROM refuel_records r WHERE r.vehicleId = vehicles.id "
                            + "ORDER BY r.date DESC, r.id DESC LIMIT 1 OFFSET 1), 0)");
                }
            };

    public abstract VehicleDao vehicleDao();

    public abstract RecordDao recordDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "carcheer.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
