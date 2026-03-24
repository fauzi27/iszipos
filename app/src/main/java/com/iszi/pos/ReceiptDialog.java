package com.iszi.pos;

import android.app.Activity;
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

        // KOP SURAT & META
        ((TextView) dialog.findViewById(R.id.tvReceiptShopName)).setText(shopName != null ? shopName.toUpperCase() : "ISZI POS");
        ((TextView) dialog.findViewById(R.id.tvReceiptAddress)).setText(shopAddress != null ? shopAddress : "Alamat Toko");
        ((TextView) dialog.findViewById(R.id.tvReceiptDate)).setText("Tgl  : " + tx.getDate());
        ((TextView) dialog.findViewById(R.id.tvReceiptInvoice)).setText("No   : " + displayInvoiceId);
        ((TextView) dialog.findViewById(R.id.tvReceiptOperator)).setText("Kasir: " + (tx.getOperatorName() != null ? tx.getOperatorName() : "Admin"));
        ((TextView) dialog.findViewById(R.id.tvReceiptBuyer)).setText("Plgn : " + (tx.getBuyer() != null ? tx.getBuyer() : "Umum"));

        // INJECT RINCIAN ITEM
        LinearLayout containerItems = dialog.findViewById(R.id.containerReceiptItems);
        containerItems.removeAllViews();
        
        int totalQty = 0;
        if (tx.getItems() != null && !tx.getItems().isEmpty()) {
            for (MenuModel item : tx.getItems()) {
                int qty = item.getStock(); totalQty += qty;

                TextView tvName = new TextView(context);
                tvName.setText(item.getName() + (isRefunded ? " (BATAL)" : ""));
                tvName.setTextColor(Color.BLACK); tvName.setTextSize(12f); tvName.setTypeface(android.graphics.Typeface.MONOSPACE);
                containerItems.addView(tvName);

                LinearLayout rowDetail = new LinearLayout(context);
                rowDetail.setOrientation(LinearLayout.HORIZONTAL); rowDetail.setWeightSum(2);

                TextView tvQtyPrice = new TextView(context);
                tvQtyPrice.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                tvQtyPrice.setText("  " + qty + "x " + formatRupiah.format(item.getPrice()).replace("Rp", ""));
                tvQtyPrice.setTextColor(Color.BLACK); tvQtyPrice.setTextSize(12f); tvQtyPrice.setTypeface(android.graphics.Typeface.MONOSPACE);

                TextView tvSubtotal = new TextView(context);
                tvSubtotal.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                tvSubtotal.setText(formatRupiah.format(item.getPrice() * qty).replace("Rp", ""));
                tvSubtotal.setTextColor(Color.BLACK); tvSubtotal.setTextSize(12f); tvSubtotal.setTypeface(android.graphics.Typeface.MONOSPACE);
                tvSubtotal.setGravity(android.view.Gravity.END);

                rowDetail.addView(tvQtyPrice); rowDetail.addView(tvSubtotal);
                containerItems.addView(rowDetail);
            }
        }

        // TOTAL & PEMBAYARAN
        ((TextView) dialog.findViewById(R.id.tvReceiptTotalItem)).setText(String.valueOf(totalQty));
        ((TextView) dialog.findViewById(R.id.tvReceiptTotal)).setText(formatRupiah.format(tx.getTotal()).replace("Rp", ""));
        ((TextView) dialog.findViewById(R.id.tvReceiptMethod)).setText(tx.getMethod() != null ? tx.getMethod() : "TUNAI");
        ((TextView) dialog.findViewById(R.id.tvReceiptPaid)).setText(formatRupiah.format(tx.getPaid()).replace("Rp", ""));
        
        LinearLayout rowChange = dialog.findViewById(R.id.rowReceiptChange);
        LinearLayout rowRemaining = dialog.findViewById(R.id.rowReceiptRemaining);
        
        if (tx.getRemaining() > 0) {
            rowChange.setVisibility(View.GONE); rowRemaining.setVisibility(View.VISIBLE);
            ((TextView) dialog.findViewById(R.id.tvReceiptRemaining)).setText(formatRupiah.format(tx.getRemaining()).replace("Rp", ""));
        } else if (tx.getPaid() > tx.getTotal()) {
            rowChange.setVisibility(View.VISIBLE); rowRemaining.setVisibility(View.GONE);
            ((TextView) dialog.findViewById(R.id.tvReceiptChange)).setText(formatRupiah.format(tx.getPaid() - tx.getTotal()).replace("Rp", ""));
        } else {
            rowChange.setVisibility(View.GONE); rowRemaining.setVisibility(View.GONE);
        }

        ((TextView) dialog.findViewById(R.id.tvReceiptFooter)).setText(shopFooter != null ? shopFooter : "Terima Kasih");
        dialog.findViewById(R.id.containerWatermark).setVisibility(removeWatermark ? View.GONE : View.VISIBLE);

        // LOGIKA TOMBOL ADMIN
        LinearLayout containerAdminActions = dialog.findViewById(R.id.containerAdminActions);
        if (containerAdminActions != null) {
            if (listener != null) {
                containerAdminActions.setVisibility(View.VISIBLE);
                MaterialButton btnReceiptRefund = dialog.findViewById(R.id.btnReceiptRefund);
                MaterialButton btnReceiptPayDebt = dialog.findViewById(R.id.btnReceiptPayDebt);
                if (isRefunded) { btnReceiptRefund.setVisibility(View.GONE); btnReceiptPayDebt.setVisibility(View.GONE); } 
                else { btnReceiptRefund.setVisibility(View.VISIBLE); btnReceiptPayDebt.setVisibility(tx.getRemaining() > 0 ? View.VISIBLE : View.GONE); }
                btnReceiptRefund.setOnClickListener(v -> { dialog.dismiss(); listener.onRefund(tx); });
                btnReceiptPayDebt.setOnClickListener(v -> { dialog.dismiss(); listener.onPayDebt(tx); });
            } else { containerAdminActions.setVisibility(View.GONE); }
        }

        dialog.findViewById(R.id.btnReceiptClose).setOnClickListener(v -> dialog.dismiss());
        
        // 🔥 MESIN CETAK PRINTER BLUETOOTH 🔥
        final int finalTotalQty = totalQty;
        dialog.findViewById(R.id.btnReceiptPrint).setOnClickListener(v -> {
            printBluetoothReceipt(context, tx, displayInvoiceId, finalTotalQty, shopName, shopAddress, shopFooter, removeWatermark);
        });

        // CETAK PDF & WA (Sama Seperti Sebelumnya)
        dialog.findViewById(R.id.btnReceiptPDF).setOnClickListener(v -> exportReceiptToPDF(context, tx, displayInvoiceId, finalTotalQty, shopName, shopAddress, shopFooter, removeWatermark));
        dialog.findViewById(R.id.btnReceiptWA).setOnClickListener(v -> {
            AlertDialog.Builder waBuilder = new AlertDialog.Builder(context);
            waBuilder.setTitle("Kirim via WhatsApp"); waBuilder.setMessage("Masukkan nomor WA Pelanggan:");
            final EditText inputWA = new EditText(context); inputWA.setHint("Cth: 08123456789"); inputWA.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            LinearLayout layoutInput = new LinearLayout(context); layoutInput.setPadding(50, 20, 50, 0); layoutInput.addView(inputWA, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            waBuilder.setView(layoutInput);
            waBuilder.setPositiveButton("Kirim", (dialogWa, which) -> {
                String phone = inputWA.getText().toString().trim();
                if (phone.isEmpty()) { Toast.makeText(context, "Nomor WA kosong!", Toast.LENGTH_SHORT).show(); return; }
                if (phone.startsWith("0")) phone = "62" + phone.substring(1);
                StringBuilder waText = new StringBuilder();
                waText.append("*STRUK PEMBELIAN - ").append(shopName != null ? shopName.toUpperCase() : "ISZI POS").append("*\n");
                waText.append("No: ").append(displayInvoiceId).append("\nTgl: ").append(tx.getDate()).append("\nKasir: ").append(tx.getOperatorName() != null ? tx.getOperatorName() : "Admin").append("\n--------------------------------\n");
                if (tx.getItems() != null) { for (MenuModel item : tx.getItems()) { waText.append(item.getName()).append("\n").append(item.getStock()).append("x Rp ").append(formatRupiah.format(item.getPrice()).replace("Rp", "")).append(" = Rp ").append(formatRupiah.format(item.getPrice() * item.getStock()).replace("Rp", "")).append("\n"); } }
                waText.append("--------------------------------\nTotal Harga: *Rp ").append(formatRupiah.format(tx.getTotal()).replace("Rp", "")).append("*\n");
                if (isRefunded) { waText.append("Status     : *❌ DIBATALKAN*\n"); } else { waText.append("Status     : *").append(tx.getMethod() != null ? tx.getMethod() : "TUNAI").append("*\n"); if (tx.getRemaining() > 0) waText.append("Sisa Hutang: Rp ").append(formatRupiah.format(tx.getRemaining()).replace("Rp", "")).append("\n"); }
                waText.append("\n").append(shopFooter != null ? shopFooter : "Terima kasih");
                if (!removeWatermark) waText.append("\n\n--------------------------------\nPowered by ISZI POS Cloud\nAplikasi Kasir Gratis & Mudah");
                try { Intent intent = new Intent(Intent.ACTION_VIEW); intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + phone + "&text=" + URLEncoder.encode(waText.toString(), "UTF-8"))); context.startActivity(intent); } catch (Exception e) { Toast.makeText(context, "Aplikasi WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show(); }
            });
            waBuilder.setNegativeButton("Batal", null); waBuilder.show();
        });

        dialog.show();
    }

    // ==========================================
    // 🔥 FUNGSI BLUETOOTH PRINTER THERMAL 🔥
    // ==========================================
    private static void printBluetoothReceipt(Context context, TransactionModel tx, String displayInvoiceId, int totalQty, String shopName, String shopAddress, String shopFooter, boolean removeWatermark) {
        Toast.makeText(context, "Menghubungkan ke Printer...", Toast.LENGTH_SHORT).show();
        
        // Operasi jaringan/bluetooth wajib dilakukan di background thread agar aplikasi tidak hang
        new Thread(() -> {
            BluetoothPrinterManager printer = new BluetoothPrinterManager(context);
            if (!printer.connect()) {
                ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Gagal konek! Pastikan printer nyala & sudah dipilih di Pengaturan.", Toast.LENGTH_LONG).show());
                return;
            }

            try {
                NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
                byte[] alignCenter = {27, 97, 1};
                byte[] alignLeft = {27, 97, 0};
                byte[] boldOn = {27, 69, 1};
                byte[] boldOff = {27, 69, 0};
                byte[] cutPaper = {29, 86, 65, 0};

                // KOP SURAT
                printer.printBytes(alignCenter);
                printer.printBytes(boldOn);
                printer.printText(shopName != null ? shopName.toUpperCase() + "\n" : "ISZI POS\n");
                printer.printBytes(boldOff);
                printer.printText(shopAddress != null ? shopAddress + "\n" : "\n");
                printer.printText("--------------------------------\n");

                // META
                printer.printBytes(alignLeft);
                printer.printText("Tgl  : " + tx.getDate() + "\n");
                printer.printText("No   : " + displayInvoiceId + "\n");
                printer.printText("Kasir: " + (tx.getOperatorName() != null ? tx.getOperatorName() : "Admin") + "\n");
                printer.printText("Plgn : " + (tx.getBuyer() != null ? tx.getBuyer() : "Umum") + "\n");
                printer.printText("--------------------------------\n");

                // ITEMS
                if (tx.getItems() != null) {
                    for (MenuModel item : tx.getItems()) {
                        String itemName = item.getName() + ("REFUNDED".equals(tx.getStatus()) ? " (BATAL)" : "");
                        if (itemName.length() > 32) itemName = itemName.substring(0, 32);
                        printer.printText(itemName + "\n");
                        
                        String leftStr = "  " + item.getStock() + "x " + formatRupiah.format(item.getPrice()).replace("Rp", "");
                        String rightStr = formatRupiah.format(item.getPrice() * item.getStock()).replace("Rp", "");
                        printer.printText(formatAlignRight(leftStr, rightStr, 32));
                    }
                }
                printer.printText("--------------------------------\n");

                // TOTAL
                printer.printText(formatAlignRight("Total Item", String.valueOf(totalQty), 32));
                printer.printBytes(boldOn);
                printer.printText(formatAlignRight("TOTAL", formatRupiah.format(tx.getTotal()).replace("Rp", ""), 32));
                printer.printBytes(boldOff);
                printer.printText(formatAlignRight("Metode", tx.getMethod() != null ? tx.getMethod() : "TUNAI", 32));
                printer.printText(formatAlignRight("Dibayar", formatRupiah.format(tx.getPaid()).replace("Rp", ""), 32));

                if (tx.getRemaining() > 0) {
                    printer.printText(formatAlignRight("Sisa Hutang", formatRupiah.format(tx.getRemaining()).replace("Rp", ""), 32));
                } else if (tx.getPaid() > tx.getTotal()) {
                    printer.printText(formatAlignRight("Kembali", formatRupiah.format(tx.getPaid() - tx.getTotal()).replace("Rp", ""), 32));
                }
                
                printer.printBytes(alignCenter);
                printer.printText("--------------------------------\n");
                printer.printText(shopFooter != null ? shopFooter + "\n" : "Terima Kasih\n");
                
                if (!removeWatermark) {
                    printer.printText("\nPowered by ISZI POS Cloud\n");
                }
                
                // Mendorong kertas naik 3 baris lalu dipotong
                printer.printText("\n\n\n");
                printer.printBytes(cutPaper);

                ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Struk berhasil dicetak!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Gagal mencetak: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                printer.disconnect();
            }
        }).start();
    }

    // Fungsi Pembantu: Membuat teks rata Kiri-Kanan untuk Printer (32 Karakter)
    private static String formatAlignRight(String leftText, String rightText, int maxLineLength) {
        int spaceLength = maxLineLength - leftText.length() - rightText.length();
        if (spaceLength <= 0) return leftText + " " + rightText + "\n"; 
        StringBuilder spaces = new StringBuilder();
        for (int i = 0; i < spaceLength; i++) spaces.append(" ");
        return leftText + spaces.toString() + rightText + "\n";
    }

    // (Fungsi Cetak PDF - Sama Seperti Sebelumnya)
    private static void exportReceiptToPDF(Context context, TransactionModel tx, String displayInvoiceId, int totalQty, String shopName, String shopAddress, String shopFooter, boolean removeWatermark) {
        Toast.makeText(context, "Menyiapkan PDF Struk...", Toast.LENGTH_SHORT).show();
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        boolean isRefunded = "REFUNDED".equals(tx.getStatus());

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>body { font-family: 'Courier New', Courier, monospace; font-size: 13px; padding: 20px; color: #000; width: 320px; margin: 0 auto; } .center { text-align: center; } .bold { font-weight: bold; } .dashed { border-bottom: 1px dashed #000; margin: 10px 0; } .row { display: table; width: 100%; } .left { display: table-cell; text-align: left; } .right { display: table-cell; text-align: right; } </style></head><body>");
        html.append("<div class='center bold' style='font-size:18px;'>").append(shopName != null ? shopName.toUpperCase() : "ISZI POS").append("</div><div class='center'>").append(shopAddress != null ? shopAddress.replace("\n", "<br>") : "").append("</div><div class='dashed'></div>");
        html.append("<div>Tgl  : ").append(tx.getDate()).append("</div><div>No   : ").append(displayInvoiceId).append("</div><div>Kasir: ").append(tx.getOperatorName() != null ? tx.getOperatorName() : "Admin").append("</div><div>Plgn : ").append(tx.getBuyer() != null ? tx.getBuyer() : "Umum").append("</div><div class='dashed'></div>");
        if (tx.getItems() != null) { for (MenuModel item : tx.getItems()) { html.append("<div>").append(item.getName()).append(isRefunded ? " (BATAL)" : "").append("</div><div class='row'><div class='left'>&nbsp;&nbsp;").append(item.getStock()).append("x ").append(formatRupiah.format(item.getPrice()).replace("Rp", "")).append("</div><div class='right'>").append(formatRupiah.format(item.getPrice() * item.getStock()).replace("Rp", "")).append("</div></div>"); } }
        html.append("<div class='dashed'></div><div class='row'><div class='left'>Total Item</div><div class='right'>").append(totalQty).append("</div></div><div class='row bold' style='font-size:15px; margin-top:5px;'><div class='left'>TOTAL</div><div class='right'>").append(formatRupiah.format(tx.getTotal()).replace("Rp", "")).append("</div></div><div class='row' style='margin-top:5px;'><div class='left'>Metode</div><div class='right'>").append(tx.getMethod() != null ? tx.getMethod() : "TUNAI").append("</div></div><div class='row'><div class='left'>Dibayar</div><div class='right'>").append(formatRupiah.format(tx.getPaid()).replace("Rp", "")).append("</div></div>");
        if (tx.getRemaining() > 0) { html.append("<div class='row bold' style='color:red;'><div class='left'>Sisa Hutang</div><div class='right'>").append(formatRupiah.format(tx.getRemaining()).replace("Rp", "")).append("</div></div>"); } else if (tx.getPaid() > tx.getTotal()) { html.append("<div class='row'><div class='left'>Kembali</div><div class='right'>").append(formatRupiah.format(tx.getPaid() - tx.getTotal()).replace("Rp", "")).append("</div></div>"); }
        html.append("<div class='dashed'></div><div class='center'>").append(shopFooter != null ? shopFooter.replace("\n", "<br>") : "Terima Kasih").append("</div>");
        if (isRefunded) html.append("<h2 class='center' style='color:red; border:2px solid red; padding:5px; transform:rotate(-10deg); width:150px; margin:20px auto;'>DIBATALKAN</h2>");
        if (!removeWatermark) html.append("<div class='center' style='font-size:10px; color:#999; margin-top:20px; border-top:1px dashed #ccc; padding-top:10px;'>Powered by ISZI POS Cloud<br>Aplikasi Kasir Gratis & Mudah</div>");
        html.append("</body></html>");
        WebView webView = new WebView(context);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
                printManager.print("Struk_" + displayInvoiceId, webView.createPrintDocumentAdapter("Struk_" + displayInvoiceId), new PrintAttributes.Builder().build());
            }
        });
        webView.loadDataWithBaseURL(null, html.toString(), "text/HTML", "UTF-8", null);
    }
}
