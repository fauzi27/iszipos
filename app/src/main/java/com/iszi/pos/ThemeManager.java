package com.iszi.pos;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;

public class ThemeManager {
    private static final String PREF_NAME = "ThemePrefs";
    private static final String KEY_THEME = "app_theme";

    public static final int THEME_AUTO = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;
    public static final int THEME_BRAND_MIEKOPIES = 3;

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

    public static void setCustomTheme(Context context) {
        int themeId = getSavedTheme(context);
        if (themeId == THEME_BRAND_MIEKOPIES) {
            context.setTheme(R.style.Theme_IsziPos_Brand);
        } else {
            context.setTheme(R.style.Theme_IsziPos);
        }
    }

    // 🔥 KUAS CAT OTOMATIS: ANTI FORCE CLOSE 🔥
    public static void paintDynamicColors(Context context, View view) {
        if (view == null) return;

        int themeId = getSavedTheme(context);
        boolean isDark = (themeId == THEME_DARK);
        boolean isBrand = (themeId == THEME_BRAND_MIEKOPIES);
        
        // Mode Terang adalah default jika bukan Dark atau Brand
        boolean isLight = (!isDark && !isBrand);

        // --- PALET WARNA ---
        int colorRoot, colorCard, colorTextMain, colorTextSub, colorBorder;

        if (isBrand) {
            colorRoot = Color.parseColor("#FFF7ED"); // brand_bg_root
            colorCard = Color.parseColor("#FFEDD5"); // brand_bg_card
            colorTextMain = Color.parseColor("#431407"); // brand_text_main
            colorTextSub = Color.parseColor("#9A3412"); // brand_text_sub
            colorBorder = Color.parseColor("#FDBA74"); // brand_border
        } else if (isLight) {
            colorRoot = Color.parseColor("#F8FAFC"); // light_bg_root
            colorCard = Color.parseColor("#FFFFFF"); // light_bg_card
            colorTextMain = Color.parseColor("#0F172A"); // light_text_main
            colorTextSub = Color.parseColor("#64748B"); // light_text_sub
            colorBorder = Color.parseColor("#E2E8F0"); // light_border
        } else {
            // Dark Mode (Bawaan)
            colorRoot = Color.parseColor("#0F172A"); // dark_bg_root
            colorCard = Color.parseColor("#1E293B"); // dark_bg_card
            colorTextMain = Color.parseColor("#FFFFFF"); // dark_text_main
            colorTextSub = Color.parseColor("#9CA3AF"); // dark_text_sub
            colorBorder = Color.parseColor("#334155"); // dark_border
        }

        applyPaintRecursively(view, colorRoot, colorCard, colorTextMain, colorTextSub, colorBorder);
    }

    private static void applyPaintRecursively(View view, int root, int card, int textMain, int textSub, int border) {
        // Cat Latar Belakang Utama (Jika view memiliki tag khusus)
        if ("bgRoot".equals(view.getTag())) {
            view.setBackgroundColor(root);
        }
        
        // Cat Latar Belakang Kartu/Panel (Jika view memiliki tag khusus)
        if ("bgCard".equals(view.getTag())) {
            if (view instanceof CardView) {
                ((CardView) view).setCardBackgroundColor(card);
            } else {
                view.setBackgroundColor(card);
            }
        }
        
        // Cat Warna Teks Utama & Sub (Berdasarkan Tag)
        if (view instanceof TextView) {
            if ("textMain".equals(view.getTag())) {
                ((TextView) view).setTextColor(textMain);
            } else if ("textSub".equals(view.getTag())) {
                ((TextView) view).setTextColor(textSub);
            }
        }

        // Cari anak-anaknya di dalam grup
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyPaintRecursively(group.getChildAt(i), root, card, textMain, textSub, border);
            }
        }
    }
}
