package com.iszi.pos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.Locale;

public class ReceiptDialog {

    public interface ReceiptActionListener {
        void onRefund(TransactionModel tx);
        void onPayDebt(TransactionModel tx);
    }

    public static void show(Context context, TransactionModel tx, String shopName, String shopAddress, String shopFooter, boolean removeWatermark) {
        show(context, tx, shopName, shopAddress, shopFooter, removeWatermark, null);
    }

    public static void show(Context context, TransactionModel tx, String shopName, String shopAddress, String shopFooter, boolean removeWatermark, ReceiptActionListener listener) {
        if (tx == null) return;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_receipt);
        
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#99000000")));
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true); 

        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        String displayInvoiceId = tx.getId().length() > 8 ? tx.getId().substring(tx.getId().length() - 8) : tx.getId();
        boolean isRefunded = "REFUNDED".equals(tx.getStatus());

        // A. KOP SURAT & META
        ((TextView) dialog.findViewById(R.id.tvReceiptShopName)).setText(shopName != null ? shopName.toUpperCase() : "ISZI POS");
        ((TextView) dialog.findViewById(R.id.tvReceiptAddress)).setText(shopAddress != null ? shopAddress : "Alamat Toko");
        ((TextView) dialog.findViewById(R.id.tvReceiptDate)).setText("Tgl  : " + tx.getDate());
        ((TextView) dialog.findViewById(R.id.tvReceiptInvoice)).setText("No   : " + displayInvoiceId);
        ((TextView) dialog.findViewById(R.id.tvReceiptOperator)).setText("Kasir: " + (tx.getOperatorName() != null ? tx.getOperatorName() : "Admin"));
        ((TextView) dialog.findViewById(R.id.tvReceiptBuyer)).setText("Plgn : " + (tx.getBuyer() != null ? tx.getBuyer() : "Umum"));

        // B. INJECT RINCIAN ITEM
        LinearLayout containerItems = dialog.findViewById(R.id.containerReceiptItems);
        containerItems.removeAllViews();
        
        int totalQty = 0;
        if (tx.getItems() != null && !tx.getItems().isEmpty()) {
            for (MenuModel item : tx.getItems()) {
                int qty = item.getStock(); 
                totalQty += qty;

                TextView tvName = new TextView(context);
                tvName.setText(item.getName() + (isRefunded ? " (BATAL)" : ""));
                tvName.setTextColor(Color.BLACK);
                tvName.setTextSize(12f);
                tvName.setTypeface(android.graphics.Typeface.MONOSPACE);
                containerItems.addView(tvName);

                LinearLayout rowDetail = new LinearLayout(context);
                rowDetail.setOrientation(LinearLayout.HORIZONTAL);
                rowDetail.setWeightSum(2);

                TextView tvQtyPrice = new TextView(context);
                tvQtyPrice.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                tvQtyPrice.setText("  " + qty + "x " + formatRupiah.format(item.getPrice()).replace("Rp", ""));
                tvQtyPrice.setTextColor(Color.BLACK);
                tvQtyPrice.setTextSize(12f);
                tvQtyPrice.setTypeface(android.graphics.Typeface.MONOSPACE);

                TextView tvSubtotal = new TextView(context);
                tvSubtotal.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                tvSubtotal.setText(formatRupiah.format(item.getPrice() * qty).replace("Rp", ""));
                tvSubtotal.setTextColor(Color.BLACK);
                tvSubtotal.setTextSize(12f);
                tvSubtotal.setTypeface(android.graphics.Typeface.MONOSPACE);
                tvSubtotal.setGravity(android.view.Gravity.END);

                rowDetail.addView(tvQtyPrice);
                rowDetail.addView(tvSubtotal);
                containerItems.addView(rowDetail);
            }
        }

        // C. TOTAL & PEMBAYARAN
        ((TextView) dialog.findViewById(R.id.tvReceiptTotalItem)).setText(String.valueOf(totalQty));
        ((TextView) dialog.findViewById(R.id.tvReceiptTotal)).setText(formatRupiah.format(tx.getTotal()).replace("Rp", ""));
        ((TextView) dialog.findViewById(R.id.tvReceiptMethod)).setText(tx.getMethod() != null ? tx.getMethod() : "TUNAI");
        ((TextView) dialog.findViewById(R.id.tvReceiptPaid)).setText(formatRupiah.format(tx.getPaid()).replace("Rp", ""));
        
        LinearLayout rowChange = dialog.findViewById(R.id.rowReceiptChange);
        LinearLayout rowRemaining = dialog.findViewById(R.id.rowReceiptRemaining);
        
        if (tx.getRemaining() > 0) {
            rowChange.setVisibility(View.GONE);
            rowRemaining.setVisibility(View.VISIBLE);
            ((TextView) dialog.findViewById(R.id.tvReceiptRemaining)).setText(formatRupiah.format(tx.getRemaining()).replace("Rp", ""));
        } else if (tx.getPaid() > tx.getTotal()) {
            rowChange.setVisibility(View.VISIBLE);
            rowRemaining.setVisibility(View.GONE);
            ((TextView) dialog.findViewById(R.id.tvReceiptChange)).setText(formatRupiah.format(tx.getPaid() - tx.getTotal()).replace("Rp", ""));
        } else {
            rowChange.setVisibility(View.GONE);
            rowRemaining.setVisibility(View.GONE);
        }

        ((TextView) dialog.findViewById(R.id.tvReceiptFooter)).setText(shopFooter != null ? shopFooter : "Terima Kasih");
        dialog.findViewById(R.id.containerWatermark).setVisibility(removeWatermark ? View.GONE : View.VISIBLE);

        // D. LOGIKA TOMBOL ADMIN
        LinearLayout containerAdminActions = dialog.findViewById(R.id.containerAdminActions);
        if (containerAdminActions != null) {
            MaterialButton btnReceiptRefund = dialog.findViewById(R.id.btnReceiptRefund);
            MaterialButton btnReceiptPayDebt = dialog.findViewById(R.id.btnReceiptPayDebt);

            if (listener != null) {
                containerAdminActions.setVisibility(View.VISIBLE);
                if (isRefunded) {
                    btnReceiptRefund.setVisibility(View.GONE);
                    btnReceiptPayDebt.setVisibility(View.GONE);
                } else {
                    btnReceiptRefund.setVisibility(View.VISIBLE);
                    btnReceiptPayDebt.setVisibility(tx.getRemaining() > 0 ? View.VISIBLE : View.GONE);
                }
                btnReceiptRefund.setOnClickListener(v -> { dialog.dismiss(); listener.onRefund(tx); });
                btnReceiptPayDebt.setOnClickListener(v -> { dialog.dismiss(); listener.onPayDebt(tx); });
            } else {
                containerAdminActions.setVisibility(View.GONE);
            }
        }

        // E. TOMBOL STANDAR
        dialog.findViewById(R.id.btnReceiptClose).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnReceiptPrint).setOnClickListener(v -> Toast.makeText(context, "Modul Printer Bluetooth sedang disiapkan.", Toast.LENGTH_SHORT).show());
        
        // 🔥 LOGIKA BARU: CETAK PDF STRUK INDIVIDU 🔥
        final int finalTotalQty = totalQty;
        dialog.findViewById(R.id.btnReceiptPDF).setOnClickListener(v -> {
            exportReceiptToPDF(context, tx, displayInvoiceId, finalTotalQty, shopName, shopAddress, shopFooter, removeWatermark);
        });

        // LOGIKA WHATSAPP
        dialog.findViewById(R.id.btnReceiptWA).setOnClickListener(v -> {
            AlertDialog.Builder waBuilder = new AlertDialog.Builder(context);
            waBuilder.setTitle("Kirim via WhatsApp");
            waBuilder.setMessage("Masukkan nomor WA Pelanggan:");
            
            final EditText inputWA = new EditText(context);
            inputWA.setHint("Cth: 08123456789");
            inputWA.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            
            LinearLayout layoutInput = new LinearLayout(context);
            layoutInput.setPadding(50, 20, 50, 0);
            layoutInput.addView(inputWA, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            waBuilder.setView(layoutInput);
            
            waBuilder.setPositiveButton("Kirim", (dialogWa, which) -> {
                String phone = inputWA.getText().toString().trim();
                if (phone.isEmpty()) { Toast.makeText(context, "Nomor WA kosong!", Toast.LENGTH_SHORT).show(); return; }
                if (phone.startsWith("0")) phone = "62" + phone.substring(1);

                StringBuilder waText = new StringBuilder();
                waText.append("*STRUK PEMBELIAN - ").append(shopName != null ? shopName.toUpperCase() : "ISZI POS").append("*\n");
                waText.append("No: ").append(displayInvoiceId).append("\n");
                waText.append("Tgl: ").append(tx.getDate()).append("\n");
                waText.append("Kasir: ").append(tx.getOperatorName() != null ? tx.getOperatorName() : "Admin").append("\n");
                waText.append("--------------------------------\n");
                
                if (tx.getItems() != null) {
                    for (MenuModel item : tx.getItems()) {
                        waText.append(item.getName()).append("\n");
                        waText.append(item.getStock()).append("x Rp ").append(formatRupiah.format(item.getPrice()).replace("Rp", ""));
                        waText.append(" = Rp ").append(formatRupiah.format(item.getPrice() * item.getStock()).replace("Rp", "")).append("\n");
                    }
                }
                
                waText.append("--------------------------------\n");
                waText.append("Total Harga: *Rp ").append(formatRupiah.format(tx.getTotal()).replace("Rp", "")).append("*\n");
                
                if (isRefunded) {
                    waText.append("Status     : *❌ DIBATALKAN*\n");
                } else {
                    waText.append("Status     : *").append(tx.getMethod() != null ? tx.getMethod() : "TUNAI").append("*\n");
                    if (tx.getRemaining() > 0) waText.append("Sisa Hutang: Rp ").append(formatRupiah.format(tx.getRemaining()).replace("Rp", "")).append("\n");
                }
                
                waText.append("\n").append(shopFooter != null ? shopFooter : "Terima kasih");
                if (!removeWatermark) waText.append("\n\n--------------------------------\nPowered by ISZI POS Cloud\nAplikasi Kasir Gratis & Mudah");

                try {
                    String url = "https://api.whatsapp.com/send?phone=" + phone + "&text=" + URLEncoder.encode(waText.toString(), "UTF-8");
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Aplikasi WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show();
                }
            });
            waBuilder.setNegativeButton("Batal", null);
            waBuilder.show();
        });

        dialog.show();
    }

    // ==========================================
    // 🔥 FUNGSI BARU: HTML TO PDF STRUK THERMAL 🔥
    // ==========================================
    private static void exportReceiptToPDF(Context context, TransactionModel tx, String displayInvoiceId, int totalQty, String shopName, String shopAddress, String shopFooter, boolean removeWatermark) {
        Toast.makeText(context, "Menyiapkan PDF Struk...", Toast.LENGTH_SHORT).show();
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        boolean isRefunded = "REFUNDED".equals(tx.getStatus());

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>")
            .append("body { font-family: 'Courier New', Courier, monospace; font-size: 13px; padding: 20px; color: #000; width: 320px; margin: 0 auto; }")
            .append(".center { text-align: center; }")
            .append(".bold { font-weight: bold; }")
            .append(".dashed { border-bottom: 1px dashed #000; margin: 10px 0; }")
            .append(".row { display: table; width: 100%; }")
            .append(".left { display: table-cell; text-align: left; }")
            .append(".right { display: table-cell; text-align: right; }")
            .append("</style></head><body>");

        // Header
        html.append("<div class='center bold' style='font-size:18px;'>").append(shopName != null ? shopName.toUpperCase() : "ISZI POS").append("</div>");
        html.append("<div class='center'>").append(shopAddress != null ? shopAddress.replace("\n", "<br>") : "").append("</div>");
        html.append("<div class='dashed'></div>");

        // Meta
        html.append("<div>Tgl  : ").append(tx.getDate()).append("</div>");
        html.append("<div>No   : ").append(displayInvoiceId).append("</div>");
        html.append("<div>Kasir: ").append(tx.getOperatorName() != null ? tx.getOperatorName() : "Admin").append("</div>");
        html.append("<div>Plgn : ").append(tx.getBuyer() != null ? tx.getBuyer() : "Umum").append("</div>");
        html.append("<div class='dashed'></div>");

        // Items
        if (tx.getItems() != null) {
            for (MenuModel item : tx.getItems()) {
                html.append("<div>").append(item.getName()).append(isRefunded ? " (BATAL)" : "").append("</div>");
                html.append("<div class='row'>")
                    .append("<div class='left'>&nbsp;&nbsp;").append(item.getStock()).append("x ").append(formatRupiah.format(item.getPrice()).replace("Rp", "")).append("</div>")
                    .append("<div class='right'>").append(formatRupiah.format(item.getPrice() * item.getStock()).replace("Rp", "")).append("</div>")
                    .append("</div>");
            }
        }
        html.append("<div class='dashed'></div>");

        // Totals
        html.append("<div class='row'><div class='left'>Total Item</div><div class='right'>").append(totalQty).append("</div></div>");
        html.append("<div class='row bold' style='font-size:15px; margin-top:5px;'><div class='left'>TOTAL</div><div class='right'>").append(formatRupiah.format(tx.getTotal()).replace("Rp", "")).append("</div></div>");
        html.append("<div class='row' style='margin-top:5px;'><div class='left'>Metode</div><div class='right'>").append(tx.getMethod() != null ? tx.getMethod() : "TUNAI").append("</div></div>");
        html.append("<div class='row'><div class='left'>Dibayar</div><div class='right'>").append(formatRupiah.format(tx.getPaid()).replace("Rp", "")).append("</div></div>");

        if (tx.getRemaining() > 0) {
            html.append("<div class='row bold' style='color:red;'><div class='left'>Sisa Hutang</div><div class='right'>").append(formatRupiah.format(tx.getRemaining()).replace("Rp", "")).append("</div></div>");
        } else if (tx.getPaid() > tx.getTotal()) {
            html.append("<div class='row'><div class='left'>Kembali</div><div class='right'>").append(formatRupiah.format(tx.getPaid() - tx.getTotal()).replace("Rp", "")).append("</div></div>");
        }

        html.append("<div class='dashed'></div>");
        html.append("<div class='center'>").append(shopFooter != null ? shopFooter.replace("\n", "<br>") : "Terima Kasih").append("</div>");

        // Batal Stamp & Watermark
        if (isRefunded) {
            html.append("<h2 class='center' style='color:red; border:2px solid red; padding:5px; transform:rotate(-10deg); width:150px; margin:20px auto;'>DIBATALKAN</h2>");
        }
        if (!removeWatermark) {
            html.append("<div class='center' style='font-size:10px; color:#999; margin-top:20px; border-top:1px dashed #ccc; padding-top:10px;'>Powered by ISZI POS Cloud<br>Aplikasi Kasir Gratis & Mudah</div>");
        }

        html.append("</body></html>");

        WebView webView = new WebView(context);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
                String jobName = "Struk_" + displayInvoiceId;
                PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
                printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
            }
        });
        webView.loadDataWithBaseURL(null, html.toString(), "text/HTML", "UTF-8", null);
    }
}
