package com.iszi.pos;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private static final String PREF_NAME = "ThemePrefs";
    private static final String KEY_THEME = "app_theme";

    public static final int THEME_AUTO = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;
    public static final int THEME_BRAND_MIEKOPIES = 3;

    // Fungsi 1: Menyimpan dan Memicu Mode Malam/Terang
    public static void applyTheme(Context context, int themeId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, themeId).apply();

        if (themeId == THEME_LIGHT || themeId == THEME_BRAND_MIEKOPIES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (themeId == THEME_DARK) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public static int getSavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, THEME_AUTO);
    }

    // Fungsi 2: Memasang Baju Tema (Harus dipanggil di setiap onCreate Activity)
    public static void setCustomTheme(Context context) {
        int themeId = getSavedTheme(context);
        if (themeId == THEME_BRAND_MIEKOPIES) {
            context.setTheme(R.style.Theme_IsziPos_Brand);
        } else {
            context.setTheme(R.style.Theme_IsziPos);
        }
    }
}
