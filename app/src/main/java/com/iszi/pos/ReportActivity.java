package com.iszi.pos;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String ownerId;

    private TextView tvDateDisplay, tvTotalOmzet, tvTotalLaba, tvTotalTunai, tvTotalHutang;
    private EditText inputSearchTx;
    private RecyclerView rvTransactions;

    private List<TransactionModel> rawTxList = new ArrayList<>();
    private List<TransactionModel> filteredTxList = new ArrayList<>();
    private TransactionAdapter txAdapter;

    private Calendar activeDate = Calendar.getInstance();
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
    private SimpleDateFormat sdfDisplay = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));

    // Info Toko untuk Struk
    private String shopName = "ISZI POS", shopAddress = "Alamat Toko", shopFooter = "Terima Kasih";
    private boolean removeWatermark = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) ownerId = currentUser.getUid();

        initViews();
        setupListeners();
        
        fetchBusinessData();
        loadTransactionsByDate(); // Load transaksi hari ini
    }

    private void initViews() {
        tvDateDisplay = findViewById(R.id.tvDateDisplay);
        tvTotalOmzet = findViewById(R.id.tvTotalOmzet);
        tvTotalLaba = findViewById(R.id.tvTotalLaba);
        tvTotalTunai = findViewById(R.id.tvTotalTunai);
        tvTotalHutang = findViewById(R.id.tvTotalHutang);
        inputSearchTx = findViewById(R.id.inputSearchTx);
        rvTransactions = findViewById(R.id.rvTransactions);

        tvDateDisplay.setText(sdfDisplay.format(activeDate.getTime()));

        txAdapter = new TransactionAdapter(filteredTxList, tx -> {
            // Tampilkan Modal Struk saat diklik
            ReceiptDialog.show(this, tx, shopName, shopAddress, shopFooter, removeWatermark);
        });
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(txAdapter);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnDatePrev).setOnClickListener(v -> {
            activeDate.add(Calendar.DAY_OF_MONTH, -1);
            tvDateDisplay.setText(sdfDisplay.format(activeDate.getTime()));
            loadTransactionsByDate();
        });

        findViewById(R.id.btnDateNext).setOnClickListener(v -> {
            activeDate.add(Calendar.DAY_OF_MONTH, 1);
            tvDateDisplay.setText(sdfDisplay.format(activeDate.getTime()));
            loadTransactionsByDate();
        });

        inputSearchTx.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applySearchFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnExportExcel).setOnClickListener(v -> Toast.makeText(this, "Fitur Excel khusus Premium!", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnExportPdf).setOnClickListener(v -> Toast.makeText(this, "Fitur PDF khusus Premium!", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnSyncCloud).setOnClickListener(v -> {
            Toast.makeText(this, "Menyinkronkan data...", Toast.LENGTH_SHORT).show();
            loadTransactionsByDate();
        });
    }

    private void fetchBusinessData() {
        if (ownerId == null) return;
        db.collection("users").document(ownerId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                if (doc.contains("shopName")) shopName = doc.getString("shopName");
                if (doc.contains("shopAddress")) shopAddress = doc.getString("shopAddress");
                if (doc.contains("receiptFooter")) shopFooter = doc.getString("receiptFooter");
                if (doc.contains("removeWatermark")) removeWatermark = doc.getBoolean("removeWatermark");
            }
        });
    }

    private void loadTransactionsByDate() {
        if (ownerId == null) return;

        // Ambil waktu dari jam 00:00:00 s/d 23:59:59 pada hari yang dipilih
        Calendar startOfDay = (Calendar) activeDate.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0); startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0); startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) activeDate.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23); endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59); endOfDay.set(Calendar.MILLISECOND, 999);

        db.collection("users").document(ownerId).collection("transactions")
                .whereGreaterThanOrEqualTo("timestamp", startOfDay.getTimeInMillis())
                .whereLessThanOrEqualTo("timestamp", endOfDay.getTimeInMillis())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    rawTxList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        TransactionModel tx = doc.toObject(TransactionModel.class);
                        tx.setId(doc.getId());
                        rawTxList.add(tx);
                    }
                    applySearchFilter(); // Terapkan pencarian dan hitung metrik
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show());
    }

    private void applySearchFilter() {
        filteredTxList.clear();
        String query = inputSearchTx.getText().toString().toLowerCase().trim();

        int totalOmzet = 0, totalTunai = 0, totalHutang = 0, totalModal = 0;

        for (TransactionModel tx : rawTxList) {
            boolean matchSearch = (tx.getBuyer() != null && tx.getBuyer().toLowerCase().contains(query)) || 
                                  (tx.getMethod() != null && tx.getMethod().toLowerCase().contains(query));
            
            if (matchSearch) {
                filteredTxList.add(tx);

                // KALKULASI METRIK (Abaikan yang statusnya BATAL/REFUNDED)
                if (!"REFUNDED".equals(tx.getStatus())) {
                    totalOmzet += tx.getTotal();
                    totalHutang += tx.getRemaining();
                    totalModal += tx.getCapitalTotal(); // Total Modal/HPP
                    
                    if (tx.getMethod() != null && tx.getMethod().toUpperCase().contains("TUNAI")) {
                        totalTunai += tx.getPaid();
                    }
                }
            }
        }
        
        int labaBersih = totalOmzet - totalModal;

        // TAMPILKAN METRIK KE DASBOR
        tvTotalOmzet.setText(formatRupiah.format(totalOmzet).replace("Rp", "Rp "));
        tvTotalTunai.setText(formatRupiah.format(totalTunai).replace("Rp", "Rp "));
        tvTotalHutang.setText(formatRupiah.format(totalHutang).replace("Rp", "Rp "));
        tvTotalLaba.setText(formatRupiah.format(labaBersih).replace("Rp", "Rp "));

        txAdapter.notifyDataSetChanged();
    }
}
