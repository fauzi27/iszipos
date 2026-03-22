package com.iszi.pos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    // Deklarasi Variabel
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private MaterialButton btnCashier, btnReport, btnManual, btnStock, btnTable, btnAdmin, btnSettings, btnLogout;
    
    private TextView tvShopName, tvShopAddress, tvRoleBadge, tvConnectionStatus;
    private MaterialButton btnCashier, btnReport, btnManual, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Inisialisasi Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Cek Sesi Login User
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // Jika belum login, tendang ke halaman Login
            goToLogin();
            return;
        }

        // 3. Hubungkan Elemen UI dari XML
        initViews();
        btnStock = findViewById(R.id.btnStock);
btnTable = findViewById(R.id.btnTable);
btnAdmin = findViewById(R.id.btnAdmin);
btnSettings = findViewById(R.id.btnSettings);
        // 4. Ambil Data Profil Toko/User dari Firestore
        fetchBusinessData(currentUser.getUid());

        // 5. Aktifkan Tombol-tombol
        setupListeners();
        btnAdmin.setOnClickListener(v -> {
    // Pindah ke Halaman Admin
    Intent intent = new Intent(MainActivity.this, AdminActivity.class);
    startActivity(intent);
});
    }

    private void initViews() {
        tvShopName = findViewById(R.id.tvShopName);
        tvShopAddress = findViewById(R.id.tvShopAddress);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);

        btnCashier = findViewById(R.id.btnCashier);
        btnReport = findViewById(R.id.btnReport);
        btnManual = findViewById(R.id.btnManual);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void fetchBusinessData(String uid) {
        // Ini adalah ekuivalen dari onSnapshot di React Native (Realtime)
        db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Toast.makeText(MainActivity.this, "Gagal sinkron data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                // Ambil data dari dokumen
                String shopName = snapshot.getString("shopName");
                String address = snapshot.getString("shopAddress");
                String role = snapshot.getString("role");
                String name = snapshot.getString("name");

                // Perbarui UI di Layar
                if (shopName != null) tvShopName.setText(shopName);
                if (address != null) {
                    tvShopAddress.setText(address);
                } else {
                    tvShopAddress.setText("Lokasi Usaha Anda");
                }

                // Cek apakah yang login adalah kasir
                if ("kasir".equals(role)) {
                    tvRoleBadge.setVisibility(View.VISIBLE);
                    String operatorName = (name != null) ? name : "Admin";
                    tvRoleBadge.setText("Kasir: " + operatorName);
                } else {
                    // Jika Owner/SuperAdmin, sembunyikan badge kasir
                    tvRoleBadge.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupListeners() {
        // Tombol Mulai Jualan (Cashier)
        btnCashier.setOnClickListener(v -> {
            // Nanti kita arahkan ke CashierActivity
            Toast.makeText(this, "Menu Kasir & Keranjang sedang dibangun...", Toast.LENGTH_SHORT).show();
        });

        // Tombol Laporan
        btnReport.setOnClickListener(v -> {
            Toast.makeText(this, "Menu Laporan sedang dibangun...", Toast.LENGTH_SHORT).show();
        });

        // Tombol Manual
        btnManual.setOnClickListener(v -> {
            Toast.makeText(this, "Menu Manual sedang dibangun...", Toast.LENGTH_SHORT).show();
        });

        // Tombol Logout
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        // Mirip seperti Alert.alert() di React Native
        new AlertDialog.Builder(this)
            .setTitle("Tutup Shift?")
            .setMessage("Anda harus memasukkan kembali user & password saat pergantian shift.")
            .setPositiveButton("Ya, Keluar", (dialog, which) -> {
                // Hapus sesi login dari HP
                auth.signOut();
                goToLogin();
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Tutup MainActivity agar user tidak bisa tekan tombol 'Back' ke lobby
    }
}
