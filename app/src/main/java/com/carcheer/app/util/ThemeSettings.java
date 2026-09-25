package com.carcheer.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.carcheer.app.R;

/** 主题颜色与语言的用户设置（SharedPreferences 持久化，重启后生效保持） */
public final class ThemeSettings {

    private static final String PREFS = "carcheer_settings";
    private static final String KEY_THEME_INDEX = "theme_index";

    /** 与 R.array.theme_names / themes.xml 变体一一对应 */
    private static final int[] THEMES = {
            R.style.Theme_CarCheer,
            R.style.Theme_CarCheer_Blue,
            R.style.Theme_CarCheer_Green,
            R.style.Theme_CarCheer_Purple,
            R.style.Theme_CarCheer_Orange,
            R.style.Theme_CarCheer_Teal
    };

    private ThemeSettings() {
    }

    public static int getThemeCount() {
        return THEMES.length;
    }

    public static int getThemeIndex(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getInt(KEY_THEME_INDEX, 0);
    }

    public static void setThemeIndex(Context context, int index) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_THEME_INDEX, index).apply();
    }

    /** 在 Activity 的 super.onCreate() 之前调用 */
    public static void applyTheme(Context context) {
        int index = getThemeIndex(context);
        ((android.app.Activity) context).setTheme(THEMES[index]);
    }
}
