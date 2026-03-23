package com.iszi.pos;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.net.URLEncoder;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiptDialog {

    public static void show(Context context, TransactionModel tx, String shopName, String shopAddress, String shopFooter, boolean removeWatermark) {
        if (tx == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_receipt, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Inisialisasi View
        TextView tvReceiptShopName = view.findViewById(R.id.tvReceiptShopName);
        TextView tvReceiptAddress = view.findViewById(R.id.tvReceiptAddress);
        TextView tvReceiptMeta = view.findViewById(R.id.tvReceiptMeta);
        TextView tvReceiptPaymentInfo = view.findViewById(R.id.tvReceiptPaymentInfo);
        TextView tvReceiptFooter = view.findViewById(R.id.tvReceiptFooter);
        TextView tvReceiptWatermark = view.findViewById(R.id.tvReceiptWatermark);
        TextView tvStampBatal = view.findViewById(R.id.tvStampBatal);
        LinearLayout containerReceiptItems = view.findViewById(R.id.containerReceiptItems);

        MaterialButton btnShareWA = view.findViewById(R.id.btnShareWA);
        MaterialButton btnSharePDF = view.findViewById(R.id.btnSharePDF);
        MaterialButton btnPrintBT = view.findViewById(R.id.btnPrintBT);
        ImageButton btnCloseReceipt = view.findViewById(R.id.btnCloseReceipt);

        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", new Locale("id", "ID"));

        // Set Data Usaha
        tvReceiptShopName.setText(shopName != null ? shopName.toUpperCase() : "ISZI POS");
        tvReceiptAddress.setText(shopAddress != null ? shopAddress : "Alamat Toko");
        tvReceiptFooter.setText(shopFooter != null ? shopFooter : "Terima kasih atas kunjungan Anda");

        if (removeWatermark) {
            tvReceiptWatermark.setVisibility(View.GONE);
        }

        // Set Status Batal
        boolean isRefunded = "REFUNDED".equals(tx.getStatus());
        if (isRefunded) {
            tvStampBatal.setVisibility(View.VISIBLE);
        }

        // Set Meta Transaksi
        long time = tx.getTimestamp() > 0 ? tx.getTimestamp() : System.currentTimeMillis();
        String invoiceId = "INV-" + time; // Disederhanakan
        
        String metaText = "Tgl  : " + sdf.format(new Date(time)) + "\n" +
                          "No   : " + invoiceId.substring(0, 14) + "\n" +
                          "Kasir: " + (tx.getOperatorName() != null ? tx.getOperatorName() : "Admin");
        
        if (tx.getBuyer() != null && !tx.getBuyer().isEmpty()) {
            metaText += "\nPlgn : " + tx.getBuyer();
        }
        tvReceiptMeta.setText(metaText);

        // Set Item Transaksi (Dinamic)
        containerReceiptItems.removeAllViews();
        int totalItemsCount = 0;
        
        // Asumsi: Karena di Java Native kita belum membuat CartItemModel khusus, 
        // kita menggunakan Item Manual sebagai placeholder jika data item kosong
        // (Nanti bisa disempurnakan jika model Cart disertakan)
        TextView tvItem = new TextView(context);
        tvItem.setText("1x Transaksi Kasir\n   Rp " + formatRupiah.format(tx.getTotal()).replace("Rp", ""));
        tvItem.setTextColor(Color.BLACK);
        tvItem.setTypeface(Typeface.MONOSPACE);
        tvItem.setTextSize(12f);
        containerReceiptItems.addView(tvItem);
        totalItemsCount = 1;

        // Set Pembayaran
        StringBuilder paymentInfo = new StringBuilder();
        paymentInfo.append("Total Item : ").append(totalItemsCount).append("\n");
        paymentInfo.append("TOTAL      : ").append(formatRupiah.format(tx.getTotal()).replace("Rp", "Rp ")).append("\n\n");
        
        if (isRefunded) {
            paymentInfo.append("Status     : DIBATALKAN\n");
        } else {
            paymentInfo.append("Metode     : ").append(tx.getMethod() != null ? tx.getMethod() : "TUNAI").append("\n");
            paymentInfo.append("Dibayar    : ").append(formatRupiah.format(tx.getPaid()).replace("Rp", "Rp ")).append("\n");
            
            if (tx.getRemaining() > 0) {
                paymentInfo.append("SISA HUTANG: ").append(formatRupiah.format(tx.getRemaining()).replace("Rp", "Rp ")).append("\n");
            }
        }
        tvReceiptPaymentInfo.setText(paymentInfo.toString());

        // LOGIKA TOMBOL
        btnCloseReceipt.setOnClickListener(v -> dialog.dismiss());

        btnSharePDF.setOnClickListener(v -> Toast.makeText(context, "Fitur cetak PDF sedang dirakit!", Toast.LENGTH_SHORT).show());
        btnPrintBT.setOnClickListener(v -> Toast.makeText(context, "Fitur printer Bluetooth sedang dirakit!", Toast.LENGTH_SHORT).show());

        btnShareWA.setOnClickListener(v -> {
            // Tampilkan Input Nomor WA
            AlertDialog.Builder waBuilder = new AlertDialog.Builder(context);
            waBuilder.setTitle("Kirim via WhatsApp");
            waBuilder.setMessage("Masukkan nomor WA Pelanggan:");
            
            final EditText inputWA = new EditText(context);
            inputWA.setHint("Cth: 08123456789");
            inputWA.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            waBuilder.setView(inputWA);
            
            waBuilder.setPositiveButton("Kirim", (dialogWa, which) -> {
                String phone = inputWA.getText().toString().trim();
                if (phone.isEmpty()) {
                    Toast.makeText(context, "Nomor WA kosong!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Format nomor (ganti 0 di depan jadi 62)
                if (phone.startsWith("0")) {
                    phone = "62" + phone.substring(1);
                }

                // Buat Teks WA
                String waText = "*STRUK PEMBELIAN - " + (shopName != null ? shopName.toUpperCase() : "ISZI") + "*\n";
                waText += "No: " + invoiceId.substring(0, 14) + "\n";
                waText += "Total: *Rp " + formatRupiah.format(tx.getTotal()).replace("Rp", "") + "*\n\n";
                waText += "Terima kasih atas kunjungan Anda.";

                try {
                    String url = "https://api.whatsapp.com/send?phone=" + phone + "&text=" + URLEncoder.encode(waText, "UTF-8");
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Gagal membuka WhatsApp", Toast.LENGTH_SHORT).show();
                }
            });
            waBuilder.setNegativeButton("Batal", null);
            waBuilder.show();
        });

        dialog.show();
    }
}
