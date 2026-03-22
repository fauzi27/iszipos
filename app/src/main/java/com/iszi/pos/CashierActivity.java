package com.iszi.pos;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CashierActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private String ownerId;

    // Menu Data
    private RecyclerView rvMenus;
    private MenuCashierAdapter menuAdapter;
    private List<MenuModel> menuList;
    private List<MenuModel> filteredMenuList;
    private EditText inputSearchMenu;

    // Cart Data
    private RecyclerView rvCart;
    private CartAdapter cartAdapter;
    private List<CartModel> cartList;
    private TextView tvCartTotal;
    private EditText inputBuyerName;
    
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashier);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) ownerId = currentUser.getUid();

        initViews();
        setupMenuRecyclerView();
        setupCartRecyclerView();
        loadMenusFromFirebase();

        // Tombol Kembali
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Tombol Reset Keranjang
        findViewById(R.id.btnResetCart).setOnClickListener(v -> clearCart());

        // Fitur Pencarian Menu
        inputSearchMenu.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterMenus(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Tombol Bayar (Checkout)
        findViewById(R.id.btnCheckout).setOnClickListener(v -> {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "Keranjang masih kosong!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Mockup Popup Bayar
            new AlertDialog.Builder(this)
                .setTitle("Pembayaran")
                .setMessage("Total Tagihan: " + tvCartTotal.getText().toString() + "\nAtas Nama: " + inputBuyerName.getText().toString())
                .setPositiveButton("Lunas (Tunai)", (dialog, which) -> {
                    Toast.makeText(this, "Pembayaran Berhasil Disimpan!", Toast.LENGTH_SHORT).show();
                    clearCart(); // Reset setelah bayar
                })
                .setNegativeButton("Batal", null)
                .show();
        });
    }

    private void initViews() {
        rvMenus = findViewById(R.id.rvMenus);
        rvCart = findViewById(R.id.rvCart);
        tvCartTotal = findViewById(R.id.tvCartTotal);
        inputSearchMenu = findViewById(R.id.inputSearchMenu);
        inputBuyerName = findViewById(R.id.inputBuyerName);
    }

    private void setupMenuRecyclerView() {
        menuList = new ArrayList<>();
        filteredMenuList = new ArrayList<>();
        
        menuAdapter = new MenuCashierAdapter(filteredMenuList, menu -> addToCart(menu));
        
        // Gunakan Grid 3 Kolom
        rvMenus.setLayoutManager(new GridLayoutManager(this, 3));
        rvMenus.setAdapter(menuAdapter);
    }

    private void setupCartRecyclerView() {
        cartList = new ArrayList<>();
        cartAdapter = new CartAdapter(cartList, (item, change) -> updateCartQty(item, change));
        
        rvCart.setLayoutManager(new LinearLayoutManager(this));
        rvCart.setAdapter(cartAdapter);
    }

    private void loadMenusFromFirebase() {
        if (ownerId == null) return;
        db.collection("users").document(ownerId).collection("menus")
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) return;
                menuList.clear();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        MenuModel m = doc.toObject(MenuModel.class);
                        m.setId(doc.getId());
                        menuList.add(m);
                    }
                }
                filterMenus(inputSearchMenu.getText().toString());
            });
    }

    private void filterMenus(String query) {
        filteredMenuList.clear();
        if (query.isEmpty()) {
            filteredMenuList.addAll(menuList);
        } else {
            for (MenuModel m : menuList) {
                if (m.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredMenuList.add(m);
                }
            }
        }
        menuAdapter.notifyDataSetChanged();
    }

    private void addToCart(MenuModel menu) {
        if (menu.getStock() == 0) {
            Toast.makeText(this, "Stok Habis!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean exists = false;
        for (CartModel c : cartList) {
            if (c.getId().equals(menu.getId())) {
                if (c.getQty() < menu.getStock()) {
                    c.setQty(c.getQty() + 1);
                } else {
                    Toast.makeText(this, "Stok tidak mencukupi!", Toast.LENGTH_SHORT).show();
                }
                exists = true;
                break;
            }
        }

        if (!exists) {
            cartList.add(new CartModel(menu.getId(), menu.getName(), menu.getPrice(), 1, menu.getStock()));
        }

        cartAdapter.notifyDataSetChanged();
        updateGrandTotal();
    }

    private void updateCartQty(CartModel item, int change) {
        int newQty = item.getQty() + change;
        
        if (newQty <= 0) {
            cartList.remove(item);
        } else if (newQty > item.getMaxStock()) {
            Toast.makeText(this, "Batas Maksimal Stok!", Toast.LENGTH_SHORT).show();
        } else {
            item.setQty(newQty);
        }
        
        cartAdapter.notifyDataSetChanged();
        updateGrandTotal();
    }

    private void clearCart() {
        cartList.clear();
        inputBuyerName.setText("");
        cartAdapter.notifyDataSetChanged();
        updateGrandTotal();
    }

    private void updateGrandTotal() {
        long total = 0;
        for (CartModel c : cartList) total += ((long) c.getPrice() * c.getQty());
        tvCartTotal.setText(formatRupiah.format(total).replace("Rp", "Rp "));
    }
}
