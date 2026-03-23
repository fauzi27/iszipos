package com.iszi.pos;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private EditText inputShopName, inputShopAddress, inputFooter;
    private MaterialButton btnSaveProfile, btnAddEmployee, btnConnectPrinter, btnBackup;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 1. Inisialisasi Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            currentUserUid = user.getUid();
        }

        // 2. Hubungkan UI
        initViews();
        
        // 3. Tarik Data Toko Lama dari Firebase
        loadUserProfile();
        
        // 4. Aktifkan Tombol
        setupListeners();
    }

    private void initViews() {
        inputShopName = findViewById(R.id.inputShopName);
        inputShopAddress = findViewById(R.id.inputShopAddress);
        inputFooter = findViewById(R.id.inputFooter);

        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnAddEmployee = findViewById(R.id.btnAddEmployee);
        btnConnectPrinter = findViewById(R.id.btnConnectPrinter);
        btnBackup = findViewById(R.id.btnBackup);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadUserProfile() {
        if (currentUserUid == null) return;

        // Tarik data secara realtime (seperti onSnapshot di React Native)
        db.collection("users").document(currentUserUid).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Toast.makeText(this, "Gagal memuat profil toko", Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                String shopName = snapshot.getString("shopName");
                String address = snapshot.getString("shopAddress");
                String footer = snapshot.getString("receiptFooter");

                if (shopName != null) inputShopName.setText(shopName);
                if (address != null) inputShopAddress.setText(address);
                if (footer != null) inputFooter.setText(footer);
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish()); // Tombol kembali

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // 🔥 SEMUA FITUR DI-UNLOCK TANPA SYARAT PREMIUM 🔥
        // (Logika kompleksnya akan kita kerjakan di fase penyempurnaan nanti)
        
        btnAddEmployee.setOnClickListener(v -> {
            Toast.makeText(this, "Fitur Multi-User Kasir terbuka untuk semua! (Segera hadir)", Toast.LENGTH_SHORT).show();
        });

        btnConnectPrinter.setOnClickListener(v -> {
            Toast.makeText(this, "Akses Printer Bluetooth terbuka untuk semua! (Segera hadir)", Toast.LENGTH_SHORT).show();
        });

        btnBackup.setOnClickListener(v -> {
            Toast.makeText(this, "Backup Excel 100% Gratis! (Mesin export sedang dirakit)", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveProfile() {
        if (currentUserUid == null) return;

        String name = inputShopName.getText().toString().trim();
        String address = inputShopAddress.getText().toString().trim();
        String footer = inputFooter.getText().toString().trim();

        if (name.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Nama dan Alamat Usaha tidak boleh kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Bungkus data yang mau diupdate
        Map<String, Object> updates = new HashMap<>();
        updates.put("shopName", name);
        updates.put("name", name); // Sinkronisasi field name lama
        updates.put("shopAddress", address);
        updates.put("address", address); // Sinkronisasi field address lama
        updates.put("receiptFooter", footer);

        // Lempar ke Firebase
        db.collection("users").document(currentUserUid).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(SettingsActivity.this, "Profil Usaha Berhasil Disimpan!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
