package com.iszi.pos;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
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
    private LinearLayout btnResetCart, btnHold, btnManual, containerCategories;
    private MaterialButton btnCheckout;
    private android.widget.ImageButton btnHoldList;

    private List<MenuModel> masterMenuList = new ArrayList<>();
    private List<MenuModel> filteredMenuList = new ArrayList<>();
    private List<MenuModel> cartList = new ArrayList<>(); 
    private List<HoldOrderModel> holdList = new ArrayList<>();
    
    // 🔥 Variabel untuk Filter Kategori
    private List<String> categoryList = new ArrayList<>();
    private String activeCategory = "all";

    private MenuCashierAdapter menuAdapter;
    private CashierCartAdapter cartAdapter;

    private int totalBelanja = 0;
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      ThemeManager.setCustomTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashier);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) ownerId = currentUser.getUid();

        initViews();
        setupListeners();
        
        fetchBusinessData();
        fetchCategories(); // 🔥 Panggil Kategori
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
        containerCategories = findViewById(R.id.containerCategories); // Menyambungkan ID Kategori

        menuAdapter = new MenuCashierAdapter(filteredMenuList, menu -> addToCart(menu));
        rvMenus.setLayoutManager(new GridLayoutManager(this, 2));
        rvMenus.setAdapter(menuAdapter);

        cartAdapter = new CashierCartAdapter(cartList, this::updateCartTotal);
        rvCart.setLayoutManager(new LinearLayoutManager(this));
        rvCart.setAdapter(cartAdapter);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnResetCart.setOnClickListener(v -> { cartList.clear(); cartAdapter.notifyDataSetChanged(); updateCartTotal(); inputBuyerName.setText(""); });
        btnCheckout.setOnClickListener(v -> { if (cartList.isEmpty()) { Toast.makeText(this, "Keranjang kosong!", Toast.LENGTH_SHORT).show(); return; } showPaymentDialog(); });
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
    // 🔥 1. FUNGSI KATEGORI DINAMIS
    // ==========================================
    private void fetchCategories() {
        if (ownerId == null) return;
        db.collection("users").document(ownerId).collection("categories")
            .get().addOnSuccessListener(snapshots -> {
                categoryList.clear();
                categoryList.add("all"); 
                for (QueryDocumentSnapshot doc : snapshots) {
                    if (doc.contains("name")) {
                        categoryList.add(doc.getString("name").toLowerCase());
                    }
                }
                renderCategoryButtons();
            });
    }

    private void renderCategoryButtons() {
        containerCategories.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String cat : categoryList) {
            View badgeView = inflater.inflate(R.layout.item_category_badge, containerCategories, false);
            LinearLayout layout = badgeView.findViewById(R.id.badgeLayout);
            TextView tvName = badgeView.findViewById(R.id.tvCategoryName);

            String displayName = cat.equals("all") ? "Semua" : cat.substring(0, 1).toUpperCase() + cat.substring(1);
            tvName.setText(displayName);

            if (activeCategory.equals(cat)) {
                layout.setBackgroundResource(R.drawable.bg_circle_button);
                layout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3B82F6")));
                tvName.setTextColor(Color.WHITE);
            } else {
                layout.setBackgroundResource(R.drawable.bg_input_modern);
                layout.setBackgroundTintList(null);
                tvName.setTextColor(Color.parseColor("#9CA3AF"));
            }

            layout.setOnClickListener(v -> {
                activeCategory = cat;
                renderCategoryButtons(); 
                applyFilter(); 
            });

            containerCategories.addView(badgeView);
        }
    }

    // ==========================================
    // 🔥 2. FILTER MENU BERSAMAAN (Pencarian + Kategori)
    // ==========================================
    private void applyFilter() {
        filteredMenuList.clear();
        String querySearch = inputSearchMenu.getText().toString().toLowerCase().trim();
        
        for (MenuModel m : masterMenuList) {
            boolean matchSearch = m.getName().toLowerCase().contains(querySearch);
            boolean matchCat = activeCategory.equals("all") || (m.getCategory() != null && m.getCategory().toLowerCase().equals(activeCategory));
            
            if (matchSearch && matchCat) {
                filteredMenuList.add(m);
            }
        }
        menuAdapter.notifyDataSetChanged();
    }

    // ==========================================
    // KODE BAWAAN (Pengambilan Data & Keranjang)
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

    // ==========================================
    // 🔥 3. FITUR MANUAL & GANTUNG
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

        inputPrice.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnAdd.setOnClickListener(v -> {
            String priceStr = inputPrice.getText().toString();
            if (priceStr.isEmpty()) { Toast.makeText(this, "Harga wajib diisi!", Toast.LENGTH_SHORT).show(); return; }
            int price = Integer.parseInt(priceStr);
            String name = inputName.getText().toString().trim();
            if (name.isEmpty()) name = "Item Manual";
            
            MenuModel manualItem = new MenuModel("MANUAL-" + System.currentTimeMillis(), name, "MANUAL", price, 1, 0);
            cartList.add(manualItem);
            cartAdapter.notifyDataSetChanged();
            updateCartTotal();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void holdCurrentTransaction() {
        if (cartList.isEmpty()) { Toast.makeText(this, "Tidak ada pesanan!", Toast.LENGTH_SHORT).show(); return; }
        String buyer = inputBuyerName.getText().toString().trim();
        if (buyer.isEmpty()) buyer = "Pelanggan " + (holdList.size() + 1);
        
        holdList.add(new HoldOrderModel(buyer, System.currentTimeMillis(), new ArrayList<>(cartList)));
        cartList.clear(); cartAdapter.notifyDataSetChanged(); updateCartTotal(); inputBuyerName.setText("");
        tvHoldBadge.setText("(" + holdList.size() + ")");
        Toast.makeText(this, "Pesanan ditahan.", Toast.LENGTH_SHORT).show();
    }

    private void showHoldListDialog() {
        if (holdList.isEmpty()) { Toast.makeText(this, "Tidak ada antrean.", Toast.LENGTH_SHORT).show(); return; }
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_hold_list);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        LinearLayout container = dialog.findViewById(R.id.containerHoldItems);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));

        for (int i = 0; i < holdList.size(); i++) {
            final int index = i; HoldOrderModel order = holdList.get(i);
            TextView tvOrder = new TextView(this);
            tvOrder.setText(order.getBuyerName() + " (" + order.getItems().size() + " Item) - Jam: " + timeFormat.format(new Date(order.getTimestamp())));
            tvOrder.setTextColor(Color.WHITE); tvOrder.setTextSize(14f); tvOrder.setPadding(30, 40, 30, 40);
            if (i % 2 == 0) tvOrder.setBackgroundColor(Color.parseColor("#334155")); else tvOrder.setBackgroundColor(Color.parseColor("#1E293B"));
            tvOrder.setOnClickListener(v -> {
                inputBuyerName.setText(order.getBuyerName());
                cartList.clear(); cartList.addAll(order.getItems());
                cartAdapter.notifyDataSetChanged(); updateCartTotal();
                holdList.remove(index); tvHoldBadge.setText("(" + holdList.size() + ")");
                dialog.dismiss();
            });
            container.addView(tvOrder);
        }
        dialog.findViewById(R.id.btnCloseHoldList).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ==========================================
    // 🔥 4. PEMBAYARAN & STRUK (Versi Update Rincian)
    // ==========================================
    private void showPaymentDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_checkout);
        
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        TextView tvCheckoutTotal = dialog.findViewById(R.id.tvCheckoutTotal);
        EditText inputCheckoutCash = dialog.findViewById(R.id.inputCheckoutCash);
        LinearLayout containerCheckoutItems = dialog.findViewById(R.id.containerCheckoutItems);
        MaterialButton btnCheckoutCancel = dialog.findViewById(R.id.btnCheckoutCancel);
        MaterialButton btnCheckoutQRIS = dialog.findViewById(R.id.btnCheckoutQRIS);
        MaterialButton btnCheckoutCash = dialog.findViewById(R.id.btnCheckoutCash);

        // Render Rincian Belanja ke dalam Pop-up
        containerCheckoutItems.removeAllViews();
        for (MenuModel item : cartList) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView tvItemName = new TextView(this);
            tvItemName.setText(item.getName());
            tvItemName.setTextColor(Color.WHITE);
            tvItemName.setTextSize(14f);
            tvItemName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvItemPrice = new TextView(this);
            int subtotal = item.getPrice() * item.getStock(); 
            tvItemPrice.setText(item.getStock() + "x " + formatRupiah.format(subtotal).replace("Rp", ""));
            tvItemPrice.setTextColor(Color.parseColor("#9CA3AF"));
            tvItemPrice.setTextSize(12f);

            row.addView(tvItemName);
            row.addView(tvItemPrice);
            containerCheckoutItems.addView(row);
        }

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
