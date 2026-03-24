package com.iszi.pos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

public class ThemeBottomSheet {

    public static void show(Context context) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_theme_bottomsheet, null);
        dialog.setContentView(view);

        MaterialButton btnThemeAuto = view.findViewById(R.id.btnThemeAuto);
        MaterialButton btnThemeDark = view.findViewById(R.id.btnThemeDark);
        MaterialButton btnThemeLight = view.findViewById(R.id.btnThemeLight);
        MaterialButton btnThemeBrand = view.findViewById(R.id.btnThemeBrand);

        btnThemeAuto.setOnClickListener(v -> {
            ThemeManager.applyTheme(context, ThemeManager.THEME_AUTO);
            dialog.dismiss();
            showRestartMessage(context);
        });

        btnThemeDark.setOnClickListener(v -> {
            ThemeManager.applyTheme(context, ThemeManager.THEME_DARK);
            dialog.dismiss();
            showRestartMessage(context);
        });

        btnThemeLight.setOnClickListener(v -> {
            ThemeManager.applyTheme(context, ThemeManager.THEME_LIGHT);
            dialog.dismiss();
            showRestartMessage(context);
        });

        btnThemeBrand.setOnClickListener(v -> {
            ThemeManager.applyTheme(context, ThemeManager.THEME_BRAND_MIEKOPIES);
            dialog.dismiss();
            showRestartMessage(context);
        });

        dialog.show();
    }

    private static void showRestartMessage(Context context) {
        Toast.makeText(context, "Tema disimpan! (Perlu adaptasi warna dinamis di pembaruan selanjutnya)", Toast.LENGTH_LONG).show();
    }
}
