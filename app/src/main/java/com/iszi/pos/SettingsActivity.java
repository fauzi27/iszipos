package com.iszi.pos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private EditText inputShopName, inputShopAddress, inputFooter;
    private SwitchCompat switchWatermark;
    private MaterialButton btnSaveProfile, btnAddEmployee, btnConnectPrinter, btnBackup;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserUid;

    // Mesin Pengekspor File Backup
    private String pendingBackupContent = "";
    private ActivityResultLauncher<Intent> backupSaveLauncher;

    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
    private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", new Locale("id", "ID"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            currentUserUid = user.getUid();
        }

        setupFileSaver();
        initViews();
        loadUserProfile();
        setupListeners();
    }

    private void setupFileSaver() {
        backupSaveLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                try {
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    os.write(pendingBackupContent.getBytes());
                    os.close();
                    Toast.makeText(this, "Backup Database Berhasil Disimpan!", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal menyimpan backup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initViews() {
        inputShopName = findViewById(R.id.inputShopName);
        inputShopAddress = findViewById(R.id.inputShopAddress);
        inputFooter = findViewById(R.id.inputFooter);
        switchWatermark = findViewById(R.id.switchWatermark);

        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnAddEmployee = findViewById(R.id.btnAddEmployee);
        btnConnectPrinter = findViewById(R.id.btnConnectPrinter);
        btnBackup = findViewById(R.id.btnBackup);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadUserProfile() {
        if (currentUserUid == null) return;

        db.collection("users").document(currentUserUid).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Toast.makeText(this, "Gagal memuat profil toko", Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                String shopName = snapshot.getString("shopName");
                String address = snapshot.getString("shopAddress");
                String footer = snapshot.getString("receiptFooter");
                Boolean removeWm = snapshot.getBoolean("removeWatermark");

                if (shopName != null) inputShopName.setText(shopName);
                if (address != null) inputShopAddress.setText(address);
                if (footer != null) inputFooter.setText(footer);
                if (removeWm != null) switchWatermark.setChecked(removeWm);
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // Fitur yang akan datang (Placeholder)
        btnAddEmployee.setOnClickListener(v -> Toast.makeText(this, "Modul Karyawan sedang dirakit.", Toast.LENGTH_SHORT).show());
        btnConnectPrinter.setOnClickListener(v -> Toast.makeText(this, "Modul Printer Bluetooth sedang dirakit.", Toast.LENGTH_SHORT).show());

        // 🔥 FITUR BACKUP DATABASE MASSAL 🔥
        btnBackup.setOnClickListener(v -> executeFullBackup());
    }

    private void saveProfile() {
        if (currentUserUid == null) return;

        String name = inputShopName.getText().toString().trim();
        String address = inputShopAddress.getText().toString().trim();
        String footer = inputFooter.getText().toString().trim();
        boolean isWatermarkRemoved = switchWatermark.isChecked();

        if (name.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Nama dan Alamat Usaha tidak boleh kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("shopName", name);
        updates.put("name", name); 
        updates.put("shopAddress", address);
        updates.put("address", address); 
        updates.put("receiptFooter", footer);
        updates.put("removeWatermark", isWatermarkRemoved); // 🔥 Simpan status Watermark

        db.collection("users").document(currentUserUid).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(SettingsActivity.this, "Profil Usaha Berhasil Disimpan!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ==========================================
    // 🔥 MESIN BACKUP DATABASE KE EXCEL (CSV) 🔥
    // ==========================================
    private void executeFullBackup() {
        if (currentUserUid == null) return;
        Toast.makeText(this, "Mengumpulkan data dari Server...", Toast.LENGTH_SHORT).show();

        StringBuilder csv = new StringBuilder();
        
        // 1. Kumpulkan Data Menu & Stok
        db.collection("users").document(currentUserUid).collection("menus").get().addOnSuccessListener(menuSnap -> {
            csv.append("--- DATA MENU & STOK ---\n");
            csv.append("ID System,Nama Menu,Kategori,Harga (Rp),Sisa Stok\n");
            
            for (QueryDocumentSnapshot doc : menuSnap) {
                String id = doc.getId();
                String nama = doc.getString("name");
                String kategori = doc.contains("category") ? doc.getString("category") : "UMUM";
                Long harga = doc.contains("price") ? doc.getLong("price") : 0L;
                Long stok = doc.contains("stock") ? doc.getLong("stock") : 0L;
                
                csv.append(id).append(",").append(nama).append(",").append(kategori).append(",")
                   .append(harga).append(",").append(stok).append("\n");
            }
            csv.append("\n\n");

            // 2. Kumpulkan Data Transaksi
            db.collection("users").document(currentUserUid).collection("transactions").get().addOnSuccessListener(txSnap -> {
                csv.append("--- DATA RIWAYAT TRANSAKSI ---\n");
                csv.append("Tanggal,Kasir,Pelanggan,Total Belanja (Rp),Dibayar (Rp),Sisa Hutang (Rp),Metode,Status\n");
                
                for (QueryDocumentSnapshot doc : txSnap) {
                    TransactionModel tx = doc.toObject(TransactionModel.class);
                    
                    String tanggal = tx.getDate() != null ? tx.getDate().replace(",", "") : sdf.format(new Date(tx.getTimestamp()));
                    String kasir = tx.getOperatorName() != null ? tx.getOperatorName() : "Admin";
                    String pelanggan = tx.getBuyer() != null ? tx.getBuyer() : "Umum";
                    String metode = tx.getMethod() != null ? tx.getMethod() : "TUNAI";
                    
                    String status = "LUNAS";
                    if ("REFUNDED".equals(tx.getStatus())) status = "BATAL";
                    else if (tx.getRemaining() > 0) status = "HUTANG";

                    csv.append(tanggal).append(",").append(kasir).append(",").append(pelanggan).append(",")
                       .append(tx.getTotal()).append(",").append(tx.getPaid()).append(",")
                       .append(tx.getRemaining()).append(",").append(metode).append(",").append(status).append("\n");
                }

                // 3. Picu Jendela Save As Android
                pendingBackupContent = csv.toString();
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/comma-separated-values");
                intent.putExtra(Intent.EXTRA_TITLE, "Backup_Database_ISZI_" + System.currentTimeMillis() + ".csv");
                backupSaveLauncher.launch(intent);

            }).addOnFailureListener(e -> Toast.makeText(this, "Gagal mengambil data Transaksi", Toast.LENGTH_SHORT).show());
        }).addOnFailureListener(e -> Toast.makeText(this, "Gagal mengambil data Menu", Toast.LENGTH_SHORT).show());
    }
}
