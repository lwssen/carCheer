package com.carcheer.app.util;

import android.content.Context;
import android.content.SharedPreferences;

/** 记录/统计页当前选中车辆的持久化。 */
public final class SelectedVehiclePrefs {

    private static final String PREFS = "carcheer_prefs";
    private static final String KEY_SELECTED_VEHICLE = "selected_vehicle_id";

    private SelectedVehiclePrefs() {
    }

    public static long get(Context context) {
        return prefs(context).getLong(KEY_SELECTED_VEHICLE, 0);
    }

    public static void set(Context context, long vehicleId) {
        prefs(context).edit().putLong(KEY_SELECTED_VEHICLE, vehicleId).apply();
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(KEY_SELECTED_VEHICLE).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
