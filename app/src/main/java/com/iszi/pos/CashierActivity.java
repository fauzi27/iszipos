package com.iszi.pos;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CashierActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String ownerId;
    private String operatorName = "Admin";

    private String shopName = "ISZI POS";
    private String shopAddress = "Alamat Toko";
    private String shopFooter = "Terima kasih";
    private boolean removeWatermark = false;

    private RecyclerView rvMenus, rvCart;
    private TextView tvCartTotal, tvHoldBadge;
    private EditText inputSearchMenu, inputBuyerName;
    private LinearLayout btnResetCart, btnHold, btnManual;
    private MaterialButton btnCheckout;

    private List<MenuModel> masterMenuList = new ArrayList<>();
    private List<MenuModel> filteredMenuList = new ArrayList<>();
    private List<MenuModel> cartList = new ArrayList<>(); 
    
    private CashierMenuAdapter menuAdapter;
    private CashierCartAdapter cartAdapter;

    private int totalBelanja = 0;
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashier);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            ownerId = currentUser.getUid();
        }

        initViews();
        setupListeners();
        
        fetchBusinessData();
        fetchMenus();
    }

    private void initViews() {
        rvMenus = findViewById(R.id.rvMenus);
        rvCart = findViewById(R.id.rvCart);
        tvCartTotal = findViewById(R.id.tvCartTotal);
        tvHoldBadge = findViewById(R.id.tvHoldBadge);
        inputSearchMenu = findViewById(R.id.inputSearchMenu);
        inputBuyerName = findViewById(R.id.inputBuyerName);
        btnResetCart = findViewById(R.id.btnResetCart);
        btnCheckout = findViewById(R.id.btnCheckout);
        btnHold = findViewById(R.id.btnHold);
        btnManual = findViewById(R.id.btnManual);

        menuAdapter = new CashierMenuAdapter(filteredMenuList, menu -> addToCart(menu));
        rvMenus.setLayoutManager(new GridLayoutManager(this, 2));
        rvMenus.setAdapter(menuAdapter);

        cartAdapter = new CashierCartAdapter(cartList, () -> updateCartTotal());
        rvCart.setLayoutManager(new LinearLayoutManager(this));
        rvCart.setAdapter(cartAdapter);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnResetCart.setOnClickListener(v -> {
            cartList.clear();
            cartAdapter.notifyDataSetChanged();
            updateCartTotal();
        });

        btnCheckout.setOnClickListener(v -> {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "Keranjang masih kosong!", Toast.LENGTH_SHORT).show();
                return;
            }
            showPaymentDialog();
        });

        btnManual.setOnClickListener(v -> Toast.makeText(this, "Fitur Manual akan segera aktif!", Toast.LENGTH_SHORT).show());
        btnHold.setOnClickListener(v -> Toast.makeText(this, "Fitur Gantung Transaksi sedang dirakit!", Toast.LENGTH_SHORT).show());

        inputSearchMenu.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchBusinessData() {
        if (ownerId == null) return;
        db.collection("users").document(ownerId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                if (doc.contains("shopName")) shopName = doc.getString("shopName");
                if (doc.contains("shopAddress")) shopAddress = doc.getString("shopAddress");
                if (doc.contains("receiptFooter")) shopFooter = doc.getString("receiptFooter");
                if (doc.contains("name")) operatorName = doc.getString("name");
                if (doc.contains("removeWatermark")) removeWatermark = doc.getBoolean("removeWatermark");
            }
        });
    }

    private void fetchMenus() {
        if (ownerId == null) return;
        db.collection("users").document(ownerId).collection("menus")
            .get().addOnSuccessListener(queryDocumentSnapshots -> {
                masterMenuList.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    MenuModel menu = doc.toObject(MenuModel.class);
                    menu.setId(doc.getId());
                    masterMenuList.add(menu);
                }
                applyFilter();
            });
    }

    private void applyFilter() {
        filteredMenuList.clear();
        String query = inputSearchMenu.getText().toString().toLowerCase().trim();
        for (MenuModel m : masterMenuList) {
            if (m.getName().toLowerCase().contains(query)) {
                filteredMenuList.add(m);
            }
        }
        menuAdapter.notifyDataSetChanged();
    }

    private void addToCart(MenuModel menu) {
        boolean exists = false;
        for (MenuModel item : cartList) {
            if (item.getId().equals(menu.getId())) {
                item.setStock(item.getStock() + 1); 
                exists = true;
                break;
            }
        }
        if (!exists) {
            MenuModel newItem = new MenuModel(menu.getId(), menu.getName(), menu.getCategory(), menu.getPrice(), 1, menu.getCapitalPrice());
            cartList.add(newItem);
        }
        cartAdapter.notifyDataSetChanged();
        updateCartTotal();
    }

    private void updateCartTotal() {
        totalBelanja = 0;
        for (MenuModel item : cartList) {
            totalBelanja += (item.getPrice() * item.getStock());
        }
        tvCartTotal.setText(formatRupiah.format(totalBelanja).replace("Rp", "Rp "));
    }

    // 🔥 INI MODAL PEMBAYARAN ELEGAN YANG BARU 🔥
    private void showPaymentDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_checkout);
        
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        TextView tvCheckoutTotal = dialog.findViewById(R.id.tvCheckoutTotal);
        EditText inputCheckoutCash = dialog.findViewById(R.id.inputCheckoutCash);
        MaterialButton btnCheckoutCancel = dialog.findViewById(R.id.btnCheckoutCancel);
        MaterialButton btnCheckoutQRIS = dialog.findViewById(R.id.btnCheckoutQRIS);
        MaterialButton btnCheckoutCash = dialog.findViewById(R.id.btnCheckoutCash);

        tvCheckoutTotal.setText(formatRupiah.format(totalBelanja).replace("Rp", "Rp "));

        btnCheckoutCancel.setOnClickListener(v -> dialog.dismiss());

        btnCheckoutQRIS.setOnClickListener(v -> {
            processTransaction(totalBelanja, "QRIS");
            dialog.dismiss();
        });

        btnCheckoutCash.setOnClickListener(v -> {
            String bayarStr = inputCheckoutCash.getText().toString();
            int dibayar = bayarStr.isEmpty() ? totalBelanja : Integer.parseInt(bayarStr);
            processTransaction(dibayar, "TUNAI");
            dialog.dismiss();
        });

        dialog.show();
    }

    private void processTransaction(int paidAmount, String method) {
        if (ownerId == null) return;

        btnCheckout.setEnabled(false);
        Toast.makeText(this, "Memproses...", Toast.LENGTH_SHORT).show();

        long timestamp = System.currentTimeMillis();
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));
        String dateStr = sdfDate.format(new Date(timestamp));
        String txId = "TX-" + timestamp;

        int remaining = totalBelanja > paidAmount ? (totalBelanja - paidAmount) : 0;
        int change = paidAmount > totalBelanja ? (paidAmount - totalBelanja) : 0;

        int capitalTotal = 0;
        // Kita juga butuh duplikat list untuk disimpan ke Firebase agar rapi
        List<MenuModel> savedItems = new ArrayList<>();
        
        for (MenuModel item : cartList) {
            capitalTotal += (item.getCapitalPrice() * item.getStock()); 
            savedItems.add(item);
        }

        String buyerName = inputBuyerName.getText().toString().trim();
        if (buyerName.isEmpty()) buyerName = "Umum";

        // 🔥 TAMBAHKAN savedItems (Daftar belanja) SAAT MEMBUAT MODEL TRANSAKSI 🔥
        TransactionModel tx = new TransactionModel(
                txId, buyerName, dateStr, method, "SUCCESS", timestamp, 
                totalBelanja, paidAmount, remaining, capitalTotal, false, savedItems
        );
        tx.setOperatorName(operatorName);
        
        Map<String, Object> txData = new HashMap<>();
        txData.put("id", tx.getId());
        txData.put("buyer", tx.getBuyer());
        txData.put("date", tx.getDate());
        txData.put("method", tx.getMethod());
        txData.put("status", tx.getStatus());
        txData.put("timestamp", tx.getTimestamp());
        txData.put("total", tx.getTotal());
        txData.put("paid", tx.getPaid());
        txData.put("remaining", tx.getRemaining());
        txData.put("change", tx.getChange()); // Perbaikan minor jika butuh field change
        txData.put("capitalTotal", tx.getCapitalTotal());
        txData.put("operatorName", tx.getOperatorName());
        txData.put("items", tx.getItems()); // 🔥 Simpan rincian belanja ke database!

        db.collection("users").document(ownerId).collection("transactions").document(txId)
                .set(txData)
                .addOnSuccessListener(aVoid -> {
                    btnCheckout.setEnabled(true);
                    
                    ReceiptDialog.show(CashierActivity.this, tx, shopName, shopAddress, shopFooter, removeWatermark);
                    
                    cartList.clear();
                    cartAdapter.notifyDataSetChanged();
                    updateCartTotal();
                    inputBuyerName.setText("");
                })
                .addOnFailureListener(e -> {
                    btnCheckout.setEnabled(true);
                    Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
