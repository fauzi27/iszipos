package com.iszi.pos;

import android.app.AlertDialog;
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
    private android.widget.ImageButton btnHoldList; // Sesuai XML milikmu

    private List<MenuModel> masterMenuList = new ArrayList<>();
    private List<MenuModel> filteredMenuList = new ArrayList<>();
    private List<MenuModel> cartList = new ArrayList<>(); 
    
    // 🔥 Kantong Penyimpanan Sementara (Fitur Gantung)
    private List<HoldOrderModel> holdList = new ArrayList<>();

    private MenuCashierAdapter menuAdapter;
    private CashierCartAdapter cartAdapter;

    private int totalBelanja = 0;
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashier);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) ownerId = currentUser.getUid();

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
        btnHoldList = findViewById(R.id.btnHoldList);

        menuAdapter = new MenuCashierAdapter(filteredMenuList, menu -> addToCart(menu));
        rvMenus.setLayoutManager(new GridLayoutManager(this, 2));
        rvMenus.setAdapter(menuAdapter);

        cartAdapter = new CashierCartAdapter(cartList, this::updateCartTotal);
        rvCart.setLayoutManager(new LinearLayoutManager(this));
        rvCart.setAdapter(cartAdapter);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnResetCart.setOnClickListener(v -> {
            cartList.clear();
            cartAdapter.notifyDataSetChanged();
            updateCartTotal();
            inputBuyerName.setText("");
        });

        btnCheckout.setOnClickListener(v -> {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "Keranjang kosong!", Toast.LENGTH_SHORT).show();
                return;
            }
            showPaymentDialog();
        });

        // 🔥 AKTIVASI TOMBOL MANUAL & GANTUNG
        btnManual.setOnClickListener(v -> showManualItemDialog());
        btnHold.setOnClickListener(v -> holdCurrentTransaction());
        btnHoldList.setOnClickListener(v -> showHoldListDialog());

        inputSearchMenu.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ==========================================
    // 🔥 1. FITUR ITEM MANUAL (OPSI 2 POS MODERN)
    // ==========================================
    private void showManualItemDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_manual_item);
        
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        EditText inputName = dialog.findViewById(R.id.inputManualName);
        EditText inputPrice = dialog.findViewById(R.id.inputManualPrice);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancelManual);
        MaterialButton btnAdd = dialog.findViewById(R.id.btnAddManual);

        // Auto-focus ke input harga saat popup muncul
        inputPrice.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String priceStr = inputPrice.getText().toString();
            if (priceStr.isEmpty()) {
                Toast.makeText(this, "Harga wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            int price = Integer.parseInt(priceStr);
            String name = inputName.getText().toString().trim();
            if (name.isEmpty()) name = "Item Manual";

            // Buat menu fiktif dan masukkan ke keranjang
            String manualId = "MANUAL-" + System.currentTimeMillis();
            MenuModel manualItem = new MenuModel(manualId, name, "MANUAL", price, 1, 0);
            
            cartList.add(manualItem);
            cartAdapter.notifyDataSetChanged();
            updateCartTotal();
            
            dialog.dismiss();
        });

        dialog.show();
    }

    // ==========================================
    // 🔥 2. FITUR GANTUNG (HOLD TRANSACTION)
    // ==========================================
    private void holdCurrentTransaction() {
        if (cartList.isEmpty()) {
            Toast.makeText(this, "Tidak ada pesanan untuk digantung!", Toast.LENGTH_SHORT).show();
            return;
        }

        String buyer = inputBuyerName.getText().toString().trim();
        if (buyer.isEmpty()) buyer = "Pelanggan " + (holdList.size() + 1);

        // Bungkus list saat ini agar tidak terikat referensi lama
        List<MenuModel> savedCart = new ArrayList<>(cartList);
        
        HoldOrderModel heldOrder = new HoldOrderModel(buyer, System.currentTimeMillis(), savedCart);
        holdList.add(heldOrder);

        // Kosongkan Kasir
        cartList.clear();
        cartAdapter.notifyDataSetChanged();
        updateCartTotal();
        inputBuyerName.setText("");

        // Update Lencana (Badge)
        tvHoldBadge.setText("(" + holdList.size() + ")");
        Toast.makeText(this, "Pesanan " + buyer + " berhasil ditahan.", Toast.LENGTH_SHORT).show();
    }

    private void showHoldListDialog() {
        if (holdList.isEmpty()) {
            Toast.makeText(this, "Tidak ada antrean pesanan.", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_hold_list);
        
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        LinearLayout container = dialog.findViewById(R.id.containerHoldItems);
        MaterialButton btnClose = dialog.findViewById(R.id.btnCloseHoldList);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));

        for (int i = 0; i < holdList.size(); i++) {
            final int index = i;
            HoldOrderModel order = holdList.get(i);

            // Bikin Kartu Antrean secara Dinamis
            TextView tvOrder = new TextView(this);
            tvOrder.setText(order.getBuyerName() + " (" + order.getItems().size() + " Item) - Jam: " + timeFormat.format(new Date(order.getTimestamp())));
            tvOrder.setTextColor(Color.WHITE);
            tvOrder.setTextSize(14f);
            tvOrder.setPadding(30, 40, 30, 40);
            
            // Background selang-seling agar rapi
            if (i % 2 == 0) tvOrder.setBackgroundColor(Color.parseColor("#334155"));
            else tvOrder.setBackgroundColor(Color.parseColor("#1E293B"));

            tvOrder.setOnClickListener(v -> {
                // Pindahkan kembali ke Kasir Utama
                inputBuyerName.setText(order.getBuyerName());
                cartList.clear();
                cartList.addAll(order.getItems());
                cartAdapter.notifyDataSetChanged();
                updateCartTotal();

                // Hapus dari daftar antrean
                holdList.remove(index);
                tvHoldBadge.setText("(" + holdList.size() + ")");
                dialog.dismiss();
            });

            container.addView(tvOrder);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ==========================================
    // KODE BAWAAN (TIDAK BERUBAH)
    // ==========================================
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
        btnCheckoutQRIS.setOnClickListener(v -> { processTransaction(totalBelanja, "QRIS"); dialog.dismiss(); });
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
        List<MenuModel> savedItems = new ArrayList<>();
        
        for (MenuModel item : cartList) {
            capitalTotal += (item.getCapitalPrice() * item.getStock()); 
            savedItems.add(item);
        }

        String buyerName = inputBuyerName.getText().toString().trim();
        if (buyerName.isEmpty()) buyerName = "Umum";

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
        txData.put("change", change); 
        txData.put("capitalTotal", tx.getCapitalTotal());
        txData.put("operatorName", tx.getOperatorName());
        txData.put("items", tx.getItems()); 

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
