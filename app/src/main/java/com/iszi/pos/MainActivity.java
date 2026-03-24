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

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    
    private TextView tvShopName, tvShopAddress, tvRoleBadge, tvConnectionStatus;
    private MaterialButton btnCashier, btnReport, btnTheme, btnStock, btnTable, btnAdmin, btnSettings, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 🔥 MESIN TEMA DINAMIS DIPANGGIL DI SINI 🔥
        ThemeManager.setCustomTheme(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        initViews();
        fetchBusinessData(currentUser.getUid());
        setupListeners();
    }

    private void initViews() {
        tvShopName = findViewById(R.id.tvShopName);
        tvShopAddress = findViewById(R.id.tvShopAddress);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);

        btnCashier = findViewById(R.id.btnCashier);
        btnReport = findViewById(R.id.btnReport);
        btnTheme = findViewById(R.id.btnTheme); 
        btnStock = findViewById(R.id.btnStock);
        btnTable = findViewById(R.id.btnTable);
        btnAdmin = findViewById(R.id.btnAdmin);
        btnSettings = findViewById(R.id.btnSettings);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void fetchBusinessData(String uid) {
        db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Toast.makeText(MainActivity.this, "Gagal sinkron data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                String shopName = snapshot.getString("shopName");
                String address = snapshot.getString("shopAddress");
                String role = snapshot.getString("role");
                String name = snapshot.getString("name");

                if (shopName != null) tvShopName.setText(shopName);
                if (address != null) tvShopAddress.setText(address);
                else tvShopAddress.setText("Lokasi Usaha Anda");

                if ("kasir".equals(role)) {
                    tvRoleBadge.setVisibility(View.VISIBLE);
                    String operatorName = (name != null) ? name : "Admin";
                    tvRoleBadge.setText("Kasir: " + operatorName);
                } else {
                    tvRoleBadge.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupListeners() {
        btnCashier.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CashierActivity.class)));
        btnAdmin.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AdminActivity.class)));
        btnReport.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ReportActivity.class)));
        btnStock.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, StockActivity.class)));
        btnTable.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TableActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
        
        btnTheme.setOnClickListener(v -> ThemeBottomSheet.show(this));
        
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        tvShopName.setOnLongClickListener(v -> {
            FirebaseUser currentUser = auth.getCurrentUser();
            String superAdminEmail = "Zii20fe@gmail.com"; 

            if (currentUser != null && currentUser.getEmail() != null) {
                if (currentUser.getEmail().equalsIgnoreCase(superAdminEmail)) {
                    Toast.makeText(MainActivity.this, "Memasuki ISZI Command Center...", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, SuperAdminActivity.class));
                }
            }
            return true; 
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Tutup Shift?")
            .setMessage("Anda harus memasukkan kembali user & password saat pergantian shift.")
            .setPositiveButton("Ya, Keluar", (dialog, which) -> {
                auth.signOut();
                goToLogin();
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); 
    }
}
