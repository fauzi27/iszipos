package com.iszi.pos;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private RecyclerView rvMenus;
    private MenuAdapter menuAdapter;
    private List<MenuModel> menuList; 
    private List<MenuModel> filteredList; 

    private EditText inputSearch;
    private MaterialButton btnAddMenu;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String ownerId; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            ownerId = currentUser.getUid(); 
        }

        initViews();
        setupRecyclerView();
        loadMenus();
        setupSearch();

        btnBack.setOnClickListener(v -> finish()); 
        
        // PANGGIL MODAL TAMBAH MENU
        btnAddMenu.setOnClickListener(v -> showMenuForm(null));
    }

    private void initViews() {
        rvMenus = findViewById(R.id.rvMenus);
        inputSearch = findViewById(R.id.inputSearch);
        btnAddMenu = findViewById(R.id.btnAddMenu);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupRecyclerView() {
        menuList = new ArrayList<>();
        filteredList = new ArrayList<>();
        
        menuAdapter = new MenuAdapter(filteredList, new MenuAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(MenuModel menu) {
                // PANGGIL MODAL EDIT MENU
                showMenuForm(menu);
            }

            @Override
            public void onDeleteClick(MenuModel menu) {
                // FUNGSI HAPUS MENU
                new AlertDialog.Builder(AdminActivity.this)
                    .setTitle("Hapus Menu?")
                    .setMessage("Yakin ingin menghapus " + menu.getName() + "?")
                    .setPositiveButton("Hapus", (dialog, which) -> {
                        db.collection("users").document(ownerId).collection("menus")
                          .document(menu.getId()).delete()
                          .addOnSuccessListener(aVoid -> Toast.makeText(AdminActivity.this, "Menu dihapus!", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Batal", null)
                    .show();
            }
        });

        rvMenus.setLayoutManager(new LinearLayoutManager(this));
        rvMenus.setAdapter(menuAdapter);
    }

    // FUNGSI MEMUNCULKAN POPUP FORM
    private void showMenuForm(MenuModel menu) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_menu_form, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Agar ujungnya melengkung rapi

        // Hubungkan elemen di dalam popup
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText inputMenuName = dialogView.findViewById(R.id.inputMenuName);
        EditText inputMenuCategory = dialogView.findViewById(R.id.inputMenuCategory);
        EditText inputMenuPrice = dialogView.findViewById(R.id.inputMenuPrice);
        EditText inputMenuCapital = dialogView.findViewById(R.id.inputMenuCapital);
        EditText inputMenuStock = dialogView.findViewById(R.id.inputMenuStock);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        // Jika mode EDIT, isi kolom dengan data lama
        if (menu != null) {
            tvDialogTitle.setText("Edit Menu");
            inputMenuName.setText(menu.getName());
            inputMenuCategory.setText(menu.getCategory());
            inputMenuPrice.setText(String.valueOf(menu.getPrice()));
            inputMenuCapital.setText(String.valueOf(menu.getCapitalPrice()));
            inputMenuStock.setText(String.valueOf(menu.getStock()));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = inputMenuName.getText().toString().trim();
            String category = inputMenuCategory.getText().toString().trim().toLowerCase();
            String priceStr = inputMenuPrice.getText().toString().trim();
            String capitalStr = inputMenuCapital.getText().toString().trim();
            String stockStr = inputMenuStock.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Nama dan Harga Jual wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Bungkus data untuk dilempar ke Firebase
            Map<String, Object> menuData = new HashMap<>();
            menuData.put("name", name);
            menuData.put("category", category.isEmpty() ? "umum" : category);
            menuData.put("price", Integer.parseInt(priceStr));
            menuData.put("capitalPrice", capitalStr.isEmpty() ? 0 : Integer.parseInt(capitalStr));
            menuData.put("stock", stockStr.isEmpty() ? 0 : Integer.parseInt(stockStr));
            menuData.put("image", ""); // Nanti kita update fitur gambarnya

            // Simpan ke Firebase
            if (menu == null) {
                // Mode Tambah Baru
                db.collection("users").document(ownerId).collection("menus").add(menuData)
                  .addOnSuccessListener(docRef -> {
                      Toast.makeText(this, "Menu berhasil ditambahkan!", Toast.LENGTH_SHORT).show();
                      dialog.dismiss();
                  });
            } else {
                // Mode Edit Lama
                db.collection("users").document(ownerId).collection("menus").document(menu.getId()).set(menuData)
                  .addOnSuccessListener(aVoid -> {
                      Toast.makeText(this, "Menu berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                      dialog.dismiss();
                  });
            }
        });

        dialog.show();
    }

    private void loadMenus() {
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
                    filterMenus(inputSearch.getText().toString());
                });
    }

    private void setupSearch() {
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterMenus(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterMenus(String text) {
        filteredList.clear();
        if (text.isEmpty()) filteredList.addAll(menuList); 
        else {
            String query = text.toLowerCase();
            for (MenuModel menu : menuList) {
                if (menu.getName() != null && menu.getName().toLowerCase().contains(query)) {
                    filteredList.add(menu);
                }
            }
        }
        menuAdapter.notifyDataSetChanged(); 
    }
}
