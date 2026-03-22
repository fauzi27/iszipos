package com.iszi.pos;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalculatorActivity extends AppCompatActivity {

    private String displayPrice = "0";
    private String displayQty = "1";
    private boolean isTypingQty = false;

    private TextView tvDisplayPrice, tvDisplayQty, tvGrandTotal;
    private EditText inputItemName;
    private LinearLayout boxPrice, boxQty, containerTags;
    private GridLayout gridNumpad;

    private RecyclerView rvManualItems;
    private ManualItemAdapter adapter;
    private List<ManualItemModel> manualItems;

    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
    private String[] quickTags = {"Camilan", "Krupuk", "Sabun", "Minuman", "Ongkir", "Lain-lain"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        initViews();
        setupRecyclerView();
        setupQuickTags();
        setupNumpad();

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnCheckout).setOnClickListener(v -> finishManualSession());
        
        // Klik Box Harga / Qty untuk pindah fokus
        boxPrice.setOnClickListener(v -> { isTypingQty = false; updateUIFocus(); });
        boxQty.setOnClickListener(v -> { isTypingQty = true; updateUIFocus(); });
    }

    private void initViews() {
        tvDisplayPrice = findViewById(R.id.tvDisplayPrice);
        tvDisplayQty = findViewById(R.id.tvDisplayQty);
        tvGrandTotal = findViewById(R.id.tvGrandTotal);
        inputItemName = findViewById(R.id.inputItemName);
        boxPrice = findViewById(R.id.boxPrice);
        boxQty = findViewById(R.id.boxQty);
        containerTags = findViewById(R.id.containerTags);
        gridNumpad = findViewById(R.id.gridNumpad);
        rvManualItems = findViewById(R.id.rvManualItems);
    }

    private void setupRecyclerView() {
        manualItems = new ArrayList<>();
        adapter = new ManualItemAdapter(manualItems, item -> {
            manualItems.remove(item);
            adapter.notifyDataSetChanged();
            updateGrandTotal();
        });
        rvManualItems.setLayoutManager(new LinearLayoutManager(this));
        rvManualItems.setAdapter(adapter);
    }

    private void setupQuickTags() {
        for (String tag : quickTags) {
            MaterialButton btnTag = new MaterialButton(this);
            btnTag.setText(tag);
            btnTag.setAllCaps(false);
            btnTag.setCornerRadius(50);
            btnTag.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#334155")));
            btnTag.setTextColor(Color.WHITE);
            btnTag.setTextSize(10);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 16, 0);
            btnTag.setLayoutParams(params);
            
            btnTag.setOnClickListener(v -> inputItemName.setText(tag));
            containerTags.addView(btnTag);
        }
    }

    private void setupNumpad() {
        String[] keys = {"7", "8", "9", "C", "4", "5", "6", "DEL", "1", "2", "3", "ADD", "0", "00", "X", ""};
        
        for (String key : keys) {
            MaterialButton btn = new MaterialButton(this);
            btn.setText(key);
            btn.setTextSize(18);
            btn.setCornerRadius(16);
            
            // Atur Warna Berdasarkan Tombol
            if (key.equals("C")) btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#7F1D1D")));
            else if (key.equals("DEL")) btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9A3412")));
            else if (key.equals("ADD")) btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#059669")));
            else if (key.equals("X")) btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#475569")));
            else btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E293B")));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 150;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            btn.setLayoutParams(params);

            if (key.isEmpty()) {
                btn.setVisibility(View.INVISIBLE);
            } else {
                btn.setOnClickListener(v -> handleNumpadPress(key));
            }
            gridNumpad.addView(btn);
        }
    }

    private void handleNumpadPress(String val) {
        if (val.equals("C")) {
            displayPrice = "0"; displayQty = "1"; isTypingQty = false;
        } else if (val.equals("DEL")) {
            if (isTypingQty) displayQty = displayQty.length() > 1 ? displayQty.substring(0, displayQty.length() - 1) : "1";
            else displayPrice = displayPrice.length() > 1 ? displayPrice.substring(0, displayPrice.length() - 1) : "0";
        } else if (val.equals("X")) {
            if (Integer.parseInt(displayPrice) > 0) {
                isTypingQty = true; displayQty = "0";
            }
        } else if (val.equals("ADD")) {
            handleAdd();
        } else {
            // Angka ditekan
            if (isTypingQty) {
                if (displayQty.equals("0") || displayQty.equals("1")) displayQty = val;
                else if (displayQty.length() < 3) displayQty += val;
            } else {
                if (displayPrice.equals("0")) displayPrice = val;
                else if (displayPrice.length() < 9) displayPrice += val;
            }
        }
        updateUIFocus();
    }

    private void handleAdd() {
        int price = Integer.parseInt(displayPrice);
        int qty = Integer.parseInt(displayQty);
        
        if (price > 0) {
            String name = inputItemName.getText().toString().trim();
            if (name.isEmpty()) name = "Item Manual";
            
            ManualItemModel newItem = new ManualItemModel("manual_" + System.currentTimeMillis(), name, price, qty);
            manualItems.add(0, newItem); // Tambah di atas
            adapter.notifyDataSetChanged();
            
            // Reset Calc
            displayPrice = "0"; displayQty = "1"; isTypingQty = false; inputItemName.setText("");
            updateGrandTotal();
            updateUIFocus();
        } else {
            Toast.makeText(this, "Input harga terlebih dahulu!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUIFocus() {
        tvDisplayPrice.setText(formatRupiah.format(Long.parseLong(displayPrice)).replace("Rp", "Rp "));
        tvDisplayQty.setText(displayQty);
        
        // Ubah warna background sebagai indikator fokus (mirip React Native)
        boxPrice.setBackgroundColor(isTypingQty ? Color.parseColor("#0F172A") : Color.parseColor("#1E3A8A"));
        boxQty.setBackgroundColor(isTypingQty ? Color.parseColor("#1E3A8A") : Color.parseColor("#0F172A"));
    }

    private void updateGrandTotal() {
        long total = 0;
        for (ManualItemModel item : manualItems) total += (long) item.getPrice() * item.getQty();
        tvGrandTotal.setText("Total: " + formatRupiah.format(total).replace("Rp", "Rp "));
    }

    // Ekuivalen AsyncStorage: Menyimpan ke SharedPreferences & JSON
    private void finishManualSession() {
        if (manualItems.isEmpty()) {
            Toast.makeText(this, "Belum ada item untuk dibayar", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SharedPreferences prefs = getSharedPreferences("ISZI_POS_PREFS", MODE_PRIVATE);
            String existingCartStr = prefs.getString("temp_manual_cart", "[]");
            JSONArray cartArray = new JSONArray(existingCartStr);

            for (ManualItemModel item : manualItems) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("name", item.getName());
                obj.put("price", item.getPrice());
                obj.put("qty", item.getQty());
                cartArray.put(obj);
            }

            prefs.edit().putString("temp_manual_cart", cartArray.toString()).apply();

            new AlertDialog.Builder(this)
                .setTitle("Berhasil")
                .setMessage(manualItems.size() + " item siap dibayar.")
                .setPositiveButton("Buka Kasir", (dialog, which) -> {
                    // Nanti kita arahkan ke CashierActivity
                    Toast.makeText(this, "Menuju Kasir...", Toast.LENGTH_SHORT).show();
                    finish(); 
                })
                .show();

        } catch (Exception e) {
            Toast.makeText(this, "Gagal menyimpan keranjang", Toast.LENGTH_SHORT).show();
        }
    }
}
