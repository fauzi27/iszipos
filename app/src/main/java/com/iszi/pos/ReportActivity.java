package com.iszi.pos;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
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

    private TextView tvDateDisplay, tvTotalOmzet, tvTotalLaba, tvTotalTunai, tvTotalQRIS, tvTotalHutang, tvTotalNota, tvLabelOmzet;
    private EditText inputSearchTx;
    private RecyclerView rvTransactions;
    private MaterialButton btnFilterDaily, btnFilterWeekly, btnFilterMonthly;

    private List<TransactionModel> rawTxList = new ArrayList<>();
    private List<TransactionModel> filteredTxList = new ArrayList<>();
    private TransactionAdapter txAdapter;

    private Calendar activeDate = Calendar.getInstance();
    private String filterMode = "daily"; // daily, weekly, monthly
    
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    private String shopName = "ISZI POS", shopAddress = "Alamat Toko", shopFooter = "Terima Kasih";
    private boolean removeWatermark = false;

    private String pendingCsvContent = "";
    private ActivityResultLauncher<Intent> csvSaveLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) ownerId = currentUser.getUid();

        setupFileSaver(); 
        initViews();
        setupListeners();
        
        fetchBusinessData();
        updateDateDisplay();
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
                    Toast.makeText(this, "Excel berhasil disimpan!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initViews() {
        tvDateDisplay = findViewById(R.id.tvDateDisplay);
        tvTotalOmzet = findViewById(R.id.tvTotalOmzet);
        tvTotalLaba = findViewById(R.id.tvTotalLaba);
        tvTotalTunai = findViewById(R.id.tvTotalTunai);
        tvTotalQRIS = findViewById(R.id.tvTotalQRIS);
        tvTotalHutang = findViewById(R.id.tvTotalHutang);
        tvTotalNota = findViewById(R.id.tvTotalNota);
        tvLabelOmzet = findViewById(R.id.tvLabelOmzet);
        
        inputSearchTx = findViewById(R.id.inputSearchTx);
        rvTransactions = findViewById(R.id.rvTransactions);
        
        btnFilterDaily = findViewById(R.id.btnFilterDaily);
        btnFilterWeekly = findViewById(R.id.btnFilterWeekly);
        btnFilterMonthly = findViewById(R.id.btnFilterMonthly);

        txAdapter = new TransactionAdapter(filteredTxList, tx -> {
            ReceiptDialog.show(this, tx, shopName, shopAddress, shopFooter, removeWatermark, new ReceiptDialog.ReceiptActionListener() {
                @Override public void onRefund(TransactionModel txData) { processRefund(txData); }
                @Override public void onPayDebt(TransactionModel txData) { processPayDebt(txData); }
            });
        });
        
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(txAdapter);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnDatePrev).setOnClickListener(v -> changeDate(-1));
        findViewById(R.id.btnDateNext).setOnClickListener(v -> changeDate(1));

        btnFilterDaily.setOnClickListener(v -> setFilterMode("daily"));
        btnFilterWeekly.setOnClickListener(v -> setFilterMode("weekly"));
        btnFilterMonthly.setOnClickListener(v -> setFilterMode("monthly"));

        inputSearchTx.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applySearchFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnSyncCloud).setOnClickListener(v -> {
            Toast.makeText(this, "Menyinkronkan data...", Toast.LENGTH_SHORT).show();
            loadTransactionsByDate();
        });

        findViewById(R.id.btnExportExcel).setOnClickListener(v -> exportToExcel());
        findViewById(R.id.btnExportPdf).setOnClickListener(v -> exportToPDF());
    }

    // ==========================================
    // 🔥 MESIN WAKTU & FILTER 🔥
    // ==========================================
    private void setFilterMode(String mode) {
        filterMode = mode;
        
        // Reset warna tombol
        int colorActiveBg = Color.parseColor("#3B82F6");
        int colorInactiveBg = Color.parseColor("#334155");
        int colorActiveText = Color.parseColor("#FFFFFF");
        int colorInactiveText = Color.parseColor("#9CA3AF");

        btnFilterDaily.setBackgroundTintList(ColorStateList.valueOf(mode.equals("daily") ? colorActiveBg : colorInactiveBg));
        btnFilterDaily.setTextColor(mode.equals("daily") ? colorActiveText : colorInactiveText);
        
        btnFilterWeekly.setBackgroundTintList(ColorStateList.valueOf(mode.equals("weekly") ? colorActiveBg : colorInactiveBg));
        btnFilterWeekly.setTextColor(mode.equals("weekly") ? colorActiveText : colorInactiveText);
        
        btnFilterMonthly.setBackgroundTintList(ColorStateList.valueOf(mode.equals("monthly") ? colorActiveBg : colorInactiveBg));
        btnFilterMonthly.setTextColor(mode.equals("monthly") ? colorActiveText : colorInactiveText);

        updateDateDisplay();
        loadTransactionsByDate();
    }

    private void changeDate(int direction) {
        if (filterMode.equals("daily")) {
            activeDate.add(Calendar.DAY_OF_MONTH, direction);
        } else if (filterMode.equals("weekly")) {
            activeDate.add(Calendar.WEEK_OF_YEAR, direction);
        } else if (filterMode.equals("monthly")) {
            activeDate.add(Calendar.MONTH, direction);
        }
        updateDateDisplay();
        loadTransactionsByDate();
    }

    private void updateDateDisplay() {
        if (filterMode.equals("daily")) {
            tvDateDisplay.setText(new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID")).format(activeDate.getTime()));
        } else if (filterMode.equals("weekly")) {
            Calendar c = (Calendar) activeDate.clone();
            c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            String startStr = new SimpleDateFormat("dd MMM", new Locale("id", "ID")).format(c.getTime());
            c.add(Calendar.DAY_OF_MONTH, 6);
            String endStr = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID")).format(c.getTime());
            tvDateDisplay.setText(startStr + " - " + endStr);
        } else if (filterMode.equals("monthly")) {
            tvDateDisplay.setText(new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID")).format(activeDate.getTime()));
        }
        tvLabelOmzet.setText("OMZET KOTOR (" + tvDateDisplay.getText().toString().toUpperCase() + ")");
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

        Calendar start = (Calendar) activeDate.clone();
        Calendar end = (Calendar) activeDate.clone();

        if (filterMode.equals("daily")) {
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0);
            end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59);
        } else if (filterMode.equals("weekly")) {
            start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0);
            end = (Calendar) start.clone();
            end.add(Calendar.DAY_OF_MONTH, 6);
            end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59);
        } else if (filterMode.equals("monthly")) {
            start.set(Calendar.DAY_OF_MONTH, 1);
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0);
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
            end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59);
        }

        db.collection("users").document(ownerId).collection("transactions")
                .whereGreaterThanOrEqualTo("timestamp", start.getTimeInMillis())
                .whereLessThanOrEqualTo("timestamp", end.getTimeInMillis())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    rawTxList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        TransactionModel tx = doc.toObject(TransactionModel.class);
                        tx.setId(doc.getId());
                        rawTxList.add(tx);
                    }
                    applySearchFilter(); 
                });
    }

    private void applySearchFilter() {
        filteredTxList.clear();
        String query = inputSearchTx.getText().toString().toLowerCase().trim();

        int totalOmzet = 0, totalTunai = 0, totalQRIS = 0, totalHutang = 0, totalModal = 0, notaSukses = 0;

        for (TransactionModel tx : rawTxList) {
            boolean matchSearch = (tx.getBuyer() != null && tx.getBuyer().toLowerCase().contains(query)) || 
                                  (tx.getMethod() != null && tx.getMethod().toLowerCase().contains(query));
            
            if (matchSearch) {
                filteredTxList.add(tx);
                if (!"REFUNDED".equals(tx.getStatus())) {
                    notaSukses++;
                    totalOmzet += tx.getTotal();
                    totalHutang += tx.getRemaining();
                    totalModal += tx.getCapitalTotal(); 
                    
                    String method = tx.getMethod() != null ? tx.getMethod().toUpperCase() : "";
                    if (method.contains("TUNAI")) totalTunai += tx.getPaid();
                    if (method.contains("QRIS")) totalQRIS += tx.getPaid();
                }
            }
        }
        
        int labaBersih = totalOmzet - totalModal;

        tvTotalOmzet.setText(formatRupiah.format(totalOmzet).replace("Rp", "Rp "));
        tvTotalTunai.setText(formatRupiah.format(totalTunai).replace("Rp", "Rp "));
        tvTotalQRIS.setText(formatRupiah.format(totalQRIS).replace("Rp", "Rp "));
        tvTotalHutang.setText(formatRupiah.format(totalHutang).replace("Rp", "Rp "));
        tvTotalLaba.setText(formatRupiah.format(labaBersih).replace("Rp", "Rp "));
        tvTotalNota.setText(notaSukses + " Nota");

        txAdapter.notifyDataSetChanged();
    }

    // ==========================================
    // 🔥 REFUND & PELUNASAN 🔥
    // ==========================================
    private void processRefund(TransactionModel tx) {
        new AlertDialog.Builder(this)
            .setTitle("Batalkan Transaksi?")
            .setMessage("Stok barang akan dikembalikan. Lanjutkan?")
            .setPositiveButton("Ya, Batalkan", (d, w) -> {
                db.collection("users").document(ownerId).collection("transactions").document(tx.getId())
                  .update("status", "REFUNDED")
                  .addOnSuccessListener(aVoid -> {
                      if (tx.getItems() != null) {
                          for (MenuModel item : tx.getItems()) {
                              if (item.getId() != null && !item.getId().startsWith("MANUAL-")) {
                                  db.collection("users").document(ownerId).collection("menus").document(item.getId())
                                    .update("stock", FieldValue.increment(item.getStock()));
                              }
                          }
                      }
                      Toast.makeText(this, "Transaksi Dibatalkan!", Toast.LENGTH_SHORT).show();
                      loadTransactionsByDate(); 
                  });
            }).setNegativeButton("Tutup", null).show();
    }

    private void processPayDebt(TransactionModel tx) {
        new AlertDialog.Builder(this)
            .setTitle("Pelunasan Hutang")
            .setMessage("Sisa: Rp " + formatRupiah.format(tx.getRemaining()).replace("Rp", "") + "\nPilih metode:")
            .setPositiveButton("TUNAI", (d, w) -> executePayDebt(tx, "TUNAI"))
            .setNegativeButton("QRIS", (d, w) -> executePayDebt(tx, "QRIS"))
            .setNeutralButton("Batal", null).show();
    }

    private void executePayDebt(TransactionModel tx, String newMethod) {
        String updatedMethod = tx.getMethod() + " + " + newMethod;
        int updatedPaid = tx.getPaid() + tx.getRemaining();

        db.collection("users").document(ownerId).collection("transactions").document(tx.getId())
          .update("remaining", 0, "paid", updatedPaid, "method", updatedMethod)
          .addOnSuccessListener(aVoid -> {
              Toast.makeText(this, "Hutang Lunas!", Toast.LENGTH_SHORT).show();
              loadTransactionsByDate(); 
          });
    }

    // ==========================================
    // 🔥 EXPORT EXCEL & PDF 🔥
    // ==========================================
    private void exportToExcel() {
        if (filteredTxList.isEmpty()) { Toast.makeText(this, "Data kosong!", Toast.LENGTH_SHORT).show(); return; }
        StringBuilder csv = new StringBuilder();
        csv.append("Tanggal,Pelanggan,Kasir,Metode,Total Belanja (Rp),Dibayar (Rp),Sisa Hutang (Rp),Status\n");
        for (TransactionModel t : filteredTxList) {
            String status = "LUNAS";
            if ("REFUNDED".equals(t.getStatus())) status = "BATAL";
            else if (t.getRemaining() > 0) status = "HUTANG";
            csv.append(t.getDate() != null ? t.getDate().replace(",", " ") : "").append(",")
               .append(t.getBuyer() != null ? t.getBuyer() : "Umum").append(",")
               .append(t.getOperatorName() != null ? t.getOperatorName() : "Admin").append(",")
               .append(t.getMethod() != null ? t.getMethod() : "TUNAI").append(",")
               .append(t.getTotal()).append(",").append(t.getPaid()).append(",")
               .append(t.getRemaining()).append(",").append(status).append("\n");
        }
        pendingCsvContent = csv.toString();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/comma-separated-values");
        intent.putExtra(Intent.EXTRA_TITLE, "Laporan_ISZI_" + System.currentTimeMillis() + ".csv");
        csvSaveLauncher.launch(intent);
    }

    private void exportToPDF() {
        if (filteredTxList.isEmpty()) { Toast.makeText(this, "Data kosong!", Toast.LENGTH_SHORT).show(); return; }
        Toast.makeText(this, "Menyiapkan PDF...", Toast.LENGTH_SHORT).show();
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:sans-serif; padding:20px; color:#333;'>")
            .append("<h2 style='text-align:center;'>Laporan Penjualan ").append(shopName.toUpperCase()).append("</h2>")
            .append("<p style='text-align:center; color:#666;'>Periode: ").append(tvDateDisplay.getText().toString()).append("</p>")
            .append("<table style='width:100%; border-collapse:collapse; margin-top:20px;' border='1'>")
            .append("<tr style='background-color:#f1f5f9; text-align:left;'>")
            .append("<th style='padding:10px;'>Tanggal</th><th style='padding:10px;'>Pelanggan</th>")
            .append("<th style='padding:10px;'>Metode</th><th style='padding:10px; text-align:right;'>Total</th>")
            .append("<th style='padding:10px;'>Status</th></tr>");

        for (TransactionModel t : filteredTxList) {
            String status = "LUNAS"; String colorStatus = "#10B981"; 
            if ("REFUNDED".equals(t.getStatus())) { status = "BATAL"; colorStatus = "#9CA3AF"; } 
            else if (t.getRemaining() > 0) { status = "HUTANG"; colorStatus = "#F59E0B"; }

            html.append("<tr>")
                .append("<td style='padding:8px;'>").append(t.getDate() != null ? t.getDate() : "-").append("</td>")
                .append("<td style='padding:8px;'>").append(t.getBuyer() != null ? t.getBuyer() : "Umum").append("</td>")
                .append("<td style='padding:8px;'>").append(t.getMethod() != null ? t.getMethod() : "TUNAI").append("</td>")
                .append("<td style='padding:8px; text-align:right;'>Rp ").append(formatRupiah.format(t.getTotal()).replace("Rp","")).append("</td>")
                .append("<td style='padding:8px; color:").append(colorStatus).append("; font-weight:bold;'>").append(status).append("</td>")
                .append("</tr>");
        }
        html.append("</table></body></html>");

        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                printManager.print("Laporan_ISZI", webView.createPrintDocumentAdapter("Laporan_ISZI"), new PrintAttributes.Builder().build());
            }
        });
        webView.loadDataWithBaseURL(null, html.toString(), "text/HTML", "UTF-8", null);
    }
}
