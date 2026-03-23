package com.iszi.pos;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

    private String shopName = "ISZI POS", shopAddress = "Alamat Toko", shopFooter = "Terima Kasih";
    private boolean removeWatermark = false;

    // 🔥 Launcher untuk Save File Excel (CSV) 🔥
    private String pendingCsvContent = "";
    private ActivityResultLauncher<Intent> csvSaveLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) ownerId = currentUser.getUid();

        initViews();
        setupListeners();
        setupFileSaver(); // Siapkan mesin penyimpan file
        
        fetchBusinessData();
        loadTransactionsByDate(); 
    }

    private void setupFileSaver() {
        csvSaveLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                try {
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    os.write(pendingCsvContent.getBytes());
                    os.close();
                    Toast.makeText(this, "File berhasil disimpan!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal menyimpan file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
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

        findViewById(R.id.btnSyncCloud).setOnClickListener(v -> {
            Toast.makeText(this, "Menyinkronkan data...", Toast.LENGTH_SHORT).show();
            loadTransactionsByDate();
        });

        // 🔥 TOMBOL EXPORT AKTIF 🔥
        findViewById(R.id.btnExportExcel).setOnClickListener(v -> exportToExcel());
        findViewById(R.id.btnExportPdf).setOnClickListener(v -> exportToPDF());
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
                    applySearchFilter(); 
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
                if (!"REFUNDED".equals(tx.getStatus())) {
                    totalOmzet += tx.getTotal();
                    totalHutang += tx.getRemaining();
                    totalModal += tx.getCapitalTotal(); 
                    if (tx.getMethod() != null && tx.getMethod().toUpperCase().contains("TUNAI")) {
                        totalTunai += tx.getPaid();
                    }
                }
            }
        }
        
        int labaBersih = totalOmzet - totalModal;

        tvTotalOmzet.setText(formatRupiah.format(totalOmzet).replace("Rp", "Rp "));
        tvTotalTunai.setText(formatRupiah.format(totalTunai).replace("Rp", "Rp "));
        tvTotalHutang.setText(formatRupiah.format(totalHutang).replace("Rp", "Rp "));
        tvTotalLaba.setText(formatRupiah.format(labaBersih).replace("Rp", "Rp "));

        txAdapter.notifyDataSetChanged();
    }

    // ==========================================
    // 🔥 FUNGSI EXPORT KE EXCEL (Format CSV) 🔥
    // ==========================================
    private void exportToExcel() {
        if (filteredTxList.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk di-export!", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Tanggal,Pelanggan,Kasir,Metode,Total Belanja (Rp),Dibayar (Rp),Sisa Hutang (Rp),Status\n");

        for (TransactionModel t : filteredTxList) {
            String status = "LUNAS";
            if ("REFUNDED".equals(t.getStatus())) status = "BATAL";
            else if (t.getRemaining() > 0) status = "HUTANG";

            String dateClean = t.getDate() != null ? t.getDate().replace(",", " ") : "";
            String buyerClean = t.getBuyer() != null ? t.getBuyer() : "Umum";
            String operatorClean = t.getOperatorName() != null ? t.getOperatorName() : "Admin";
            String methodClean = t.getMethod() != null ? t.getMethod() : "TUNAI";

            csv.append(dateClean).append(",");
            csv.append(buyerClean).append(",");
            csv.append(operatorClean).append(",");
            csv.append(methodClean).append(",");
            csv.append(t.getTotal()).append(",");
            csv.append(t.getPaid()).append(",");
            csv.append(t.getRemaining()).append(",");
            csv.append(status).append("\n");
        }

        // Panggil jendela 'Save As' Android
        pendingCsvContent = csv.toString();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/comma-separated-values");
        intent.putExtra(Intent.EXTRA_TITLE, "Laporan_Penjualan_" + System.currentTimeMillis() + ".csv");
        csvSaveLauncher.launch(intent);
    }

    // ==========================================
    // 🔥 FUNGSI EXPORT KE PDF 🔥
    // ==========================================
    private void exportToPDF() {
        if (filteredTxList.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk di-export!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Menyiapkan PDF...", Toast.LENGTH_SHORT).show();

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:sans-serif; padding:20px; color:#333;'>");
        html.append("<h2 style='text-align:center;'>Laporan Penjualan ").append(shopName.toUpperCase()).append("</h2>");
        html.append("<p style='text-align:center; color:#666;'>Periode: ").append(tvDateDisplay.getText().toString()).append("</p>");
        
        html.append("<table style='width:100%; border-collapse:collapse; margin-top:20px;' border='1'>");
        html.append("<tr style='background-color:#f1f5f9; text-align:left;'>");
        html.append("<th style='padding:10px;'>Tanggal</th>");
        html.append("<th style='padding:10px;'>Pelanggan</th>");
        html.append("<th style='padding:10px;'>Metode</th>");
        html.append("<th style='padding:10px; text-align:right;'>Total</th>");
        html.append("<th style='padding:10px;'>Status</th>");
        html.append("</tr>");

        for (TransactionModel t : filteredTxList) {
            String status = "LUNAS";
            String colorStatus = "#10B981"; // Hijau
            if ("REFUNDED".equals(t.getStatus())) {
                status = "BATAL"; colorStatus = "#9CA3AF"; // Abu-abu
            } else if (t.getRemaining() > 0) {
                status = "HUTANG"; colorStatus = "#F59E0B"; // Oranye
            }

            html.append("<tr>");
            html.append("<td style='padding:8px;'>").append(t.getDate() != null ? t.getDate() : "-").append("</td>");
            html.append("<td style='padding:8px;'>").append(t.getBuyer() != null ? t.getBuyer() : "Umum").append("</td>");
            html.append("<td style='padding:8px;'>").append(t.getMethod() != null ? t.getMethod() : "TUNAI").append("</td>");
            html.append("<td style='padding:8px; text-align:right;'>Rp ").append(formatRupiah.format(t.getTotal()).replace("Rp","")).append("</td>");
            html.append("<td style='padding:8px; color:").append(colorStatus).append("; font-weight:bold;'>").append(status).append("</td>");
            html.append("</tr>");
        }
        html.append("</table></body></html>");

        // Proses cetak HTML menjadi PDF menggunakan PrintManager Android
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                String jobName = "Laporan_ISZI_" + System.currentTimeMillis();
                PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
                printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
            }
        });
        webView.loadDataWithBaseURL(null, html.toString(), "text/HTML", "UTF-8", null);
    }
}
