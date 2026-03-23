package com.iszi.pos;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String ownerId;
    private boolean isPremium = false; // Simulasi akun premium

    private TextView tvDateDisplay, tvTotalOmzet, tvTotalLaba, tvTotalTunai, tvTotalHutang;
    private EditText inputSearchTx;
    private RecyclerView rvTransactions;

    private TransactionAdapter adapter;
    private List<TransactionModel> masterList;
    private List<TransactionModel> filteredList;

    private Calendar activeDate;
    private SimpleDateFormat dateFormat;
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) ownerId = currentUser.getUid();

        activeDate = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));

        initViews();
        setupRecyclerView();
        setupListeners();
        
        updateDateDisplay();
        loadTransactionsFromFirebase();
    }

    private void initViews() {
        tvDateDisplay = findViewById(R.id.tvDateDisplay);
        tvTotalOmzet = findViewById(R.id.tvTotalOmzet);
        tvTotalLaba = findViewById(R.id.tvTotalLaba);
        tvTotalTunai = findViewById(R.id.tvTotalTunai);
        tvTotalHutang = findViewById(R.id.tvTotalHutang);
        inputSearchTx = findViewById(R.id.inputSearchTx);
        rvTransactions = findViewById(R.id.rvTransactions);
    }

    private void setupRecyclerView() {
        masterList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new TransactionAdapter(filteredList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnDatePrev).setOnClickListener(v -> {
            activeDate.add(Calendar.DAY_OF_MONTH, -1);
            updateDateDisplay();
            applyFilters();
        });

        findViewById(R.id.btnDateNext).setOnClickListener(v -> {
            activeDate.add(Calendar.DAY_OF_MONTH, 1);
            updateDateDisplay();
            applyFilters();
        });

        findViewById(R.id.btnSyncCloud).setOnClickListener(v -> {
            Toast.makeText(this, "Menarik data dari Cloud...", Toast.LENGTH_SHORT).show();
            loadTransactionsFromFirebase();
        });

        // Simulasi Paywall Export
        findViewById(R.id.btnExportExcel).setOnClickListener(v -> handleExport("Excel"));
        findViewById(R.id.btnExportPdf).setOnClickListener(v -> handleExport("PDF"));

        inputSearchTx.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void handleExport(String type) {
        if (!isPremium) {
            Toast.makeText(this, "Akses " + type + " dikunci! Silakan upgrade ke Premium.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Mengekspor " + type + "...", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDateDisplay() {
        // Cek apakah hari ini
        Calendar today = Calendar.getInstance();
        if (activeDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            activeDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            tvDateDisplay.setText("Hari Ini");
        } else {
            tvDateDisplay.setText(dateFormat.format(activeDate.getTime()));
        }
    }

    private void loadTransactionsFromFirebase() {
        if (ownerId == null) return;
        // Sementara kita tarik semua dari Firebase (mirip fungsi handleFetchFromCloud di React)
        db.collection("users").document(ownerId).collection("transactions")
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) return;
                masterList.clear();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        TransactionModel t = doc.toObject(TransactionModel.class);
                        t.setId(doc.getId());
                        masterList.add(t);
                    }
                }
                applyFilters();
            });
    }

    private void applyFilters() {
        filteredList.clear();
        String query = inputSearchTx.getText().toString().toLowerCase().trim();
        
        long omzet = 0, laba = 0, tunai = 0, hutang = 0;

        for (TransactionModel t : masterList) {
            // 1. Filter Tanggal (Sederhana: Cocokkan String tanggal)
            // Catatan: Ini cara sederhana, idealnya menggunakan timestamp
            String dateStr = dateFormat.format(t.getTimestamp() > 0 ? t.getTimestamp() : System.currentTimeMillis());
            String activeDateStr = dateFormat.format(activeDate.getTime());
            
            boolean matchDate = dateStr.equals(activeDateStr);
            boolean matchSearch = t.getBuyer() != null && t.getBuyer().toLowerCase().contains(query);

            if (matchDate && matchSearch) {
                filteredList.add(t);
                
                // 2. Hitung Metrik Keuangan
                if (!"REFUNDED".equals(t.getStatus())) {
                    omzet += t.getTotal();
                    hutang += t.getRemaining();
                    laba += (t.getTotal() - t.getCapitalTotal());
                    
                    if (t.getMethod() != null && t.getMethod().contains("TUNAI")) {
                        tunai += t.getPaid();
                    }
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        
        // Update Teks Layar
        tvTotalOmzet.setText(formatRupiah.format(omzet).replace("Rp", "Rp "));
        tvTotalLaba.setText(formatRupiah.format(laba).replace("Rp", "Rp "));
        tvTotalTunai.setText(formatRupiah.format(tunai).replace("Rp", "Rp "));
        tvTotalHutang.setText(formatRupiah.format(hutang).replace("Rp", "Rp "));
    }
}
