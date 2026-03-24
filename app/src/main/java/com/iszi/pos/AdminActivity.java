package com.iszi.pos;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
    private LinearLayout containerCategories;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String ownerId; 

    // Variabel Kategori
    private List<String> categoryList = new ArrayList<>();
    private String activeCategory = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      ThemeManager.setCustomTheme(this);
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
        
        fetchCategories();
        loadMenus();
        setupSearch();

        btnBack.setOnClickListener(v -> finish()); 
        btnAddMenu.setOnClickListener(v -> showMenuForm(null));
        
        findViewById(R.id.btnExport).setOnClickListener(v -> Toast.makeText(this, "Fitur Export sedang dirakit!", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnImport).setOnClickListener(v -> Toast.makeText(this, "Fitur Import CSV sedang dirakit!", Toast.LENGTH_SHORT).show());
    }

    private void initViews() {
        rvMenus = findViewById(R.id.rvMenus);
        inputSearch = findViewById(R.id.inputSearch);
        btnAddMenu = findViewById(R.id.btnAddMenu);
        btnBack = findViewById(R.id.btnBack);
        containerCategories = findViewById(R.id.containerCategories); 
    }

    // ==========================================
    // 🔥 1. KATEGORI & FILTERING
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
                layout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EA580C")));
                tvName.setTextColor(Color.WHITE);
            } else {
                layout.setBackgroundResource(R.drawable.bg_input_modern);
                layout.setBackgroundTintList(null);
                tvName.setTextColor(Color.parseColor("#9CA3AF"));
            }

            layout.setOnClickListener(v -> {
                activeCategory = cat;
                renderCategoryButtons(); 
                filterMenus(); 
            });

            containerCategories.addView(badgeView);
        }
    }

    private void filterMenus() {
        filteredList.clear();
        String querySearch = inputSearch.getText().toString().toLowerCase().trim();
        
        for (MenuModel m : menuList) {
            boolean matchSearch = m.getName() != null && m.getName().toLowerCase().contains(querySearch);
            boolean matchCat = activeCategory.equals("all") || (m.getCategory() != null && m.getCategory().toLowerCase().equals(activeCategory));
            
            if (matchSearch && matchCat) {
                filteredList.add(m);
            }
        }
        menuAdapter.notifyDataSetChanged();
    }

    private void setupSearch() {
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterMenus(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ==========================================
    // 🔥 2. DAFTAR MENU & FORM TAMBAH/EDIT
    // ==========================================
    private void setupRecyclerView() {
        menuList = new ArrayList<>();
        filteredList = new ArrayList<>();
        
        menuAdapter = new MenuAdapter(filteredList, new MenuAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(MenuModel menu) { showMenuForm(menu); }

            @Override
            public void onDeleteClick(MenuModel menu) {
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

    // 🔥 FORM MENU YANG SUDAH DI-UPGRADE DENGAN DROPDOWN 🔥
    private void showMenuForm(MenuModel menu) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_menu_form, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText inputMenuName = dialogView.findViewById(R.id.inputMenuName);
        EditText inputMenuPrice = dialogView.findViewById(R.id.inputMenuPrice);
        EditText inputMenuCapital = dialogView.findViewById(R.id.inputMenuCapital);
        EditText inputMenuStock = dialogView.findViewById(R.id.inputMenuStock);
        
        Spinner spinnerMenuCategory = dialogView.findViewById(R.id.spinnerMenuCategory);
        MaterialButton btnAddCategory = dialogView.findViewById(R.id.btnAddCategory);
        
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        // 1. Siapkan data untuk Dropdown (Spinner)
        List<String> spinnerCats = new ArrayList<>();
        for (String c : categoryList) {
            if (!c.equals("all")) { // Singkirkan "all" dari pilihan form
                spinnerCats.add(c.substring(0, 1).toUpperCase() + c.substring(1));
            }
        }
        if (spinnerCats.isEmpty()) spinnerCats.add("Umum"); // Default jika kosong

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerCats);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMenuCategory.setAdapter(spinnerAdapter);

        // 2. Tombol Tambah Kategori Baru (Muncul Pop-up kecil)
        btnAddCategory.setOnClickListener(v -> {
            AlertDialog.Builder catBuilder = new AlertDialog.Builder(this);
            catBuilder.setTitle("Tambah Kategori");
            final EditText inputCat = new EditText(this);
            inputCat.setHint("Cth: Snack");
            catBuilder.setView(inputCat);
            
            catBuilder.setPositiveButton("Simpan", (d, w) -> {
                String newCat = inputCat.getText().toString().trim();
                if (!newCat.isEmpty()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", newCat.toLowerCase());
                    db.collection("users").document(ownerId).collection("categories").add(data)
                        .addOnSuccessListener(docRef -> {
                            // Masukkan ke dropdown & langsung pilih
                            String displayCat = newCat.substring(0, 1).toUpperCase() + newCat.substring(1).toLowerCase();
                            spinnerAdapter.add(displayCat);
                            spinnerAdapter.notifyDataSetChanged();
                            spinnerMenuCategory.setSelection(spinnerAdapter.getCount() - 1);
                            
                            fetchCategories(); // Refresh latar belakang
                            Toast.makeText(this, "Kategori ditambahkan!", Toast.LENGTH_SHORT).show();
                        });
                }
            });
            catBuilder.setNegativeButton("Batal", null);
            catBuilder.show();
        });

        // 3. Isi data jika mode EDIT
        if (menu != null) {
            tvDialogTitle.setText("Edit Menu");
            inputMenuName.setText(menu.getName());
            inputMenuPrice.setText(String.valueOf(menu.getPrice()));
            inputMenuCapital.setText(String.valueOf(menu.getCapitalPrice()));
            inputMenuStock.setText(String.valueOf(menu.getStock()));

            // Cari kategori menu ini di Dropdown dan pilih otomatis
            if (menu.getCategory() != null) {
                String catCapped = menu.getCategory().substring(0, 1).toUpperCase() + menu.getCategory().substring(1);
                int spinnerPos = spinnerAdapter.getPosition(catCapped);
                if (spinnerPos >= 0) spinnerMenuCategory.setSelection(spinnerPos);
            }
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = inputMenuName.getText().toString().trim();
            String priceStr = inputMenuPrice.getText().toString().trim();
            String capitalStr = inputMenuCapital.getText().toString().trim();
            String stockStr = inputMenuStock.getText().toString().trim();
            
            // Ambil dari Dropdown
            String category = spinnerMenuCategory.getSelectedItem() != null ? 
                              spinnerMenuCategory.getSelectedItem().toString().toLowerCase() : "umum";

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Nama dan Harga Jual wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> menuData = new HashMap<>();
            menuData.put("name", name);
            menuData.put("category", category);
            menuData.put("price", Integer.parseInt(priceStr));
            menuData.put("capitalPrice", capitalStr.isEmpty() ? 0 : Integer.parseInt(capitalStr));
            menuData.put("stock", stockStr.isEmpty() ? 0 : Integer.parseInt(stockStr));
            menuData.put("image", ""); 

            if (menu == null) {
                db.collection("users").document(ownerId).collection("menus").add(menuData)
                  .addOnSuccessListener(docRef -> {
                      Toast.makeText(this, "Menu berhasil ditambahkan!", Toast.LENGTH_SHORT).show();
                      dialog.dismiss();
                  });
            } else {
                db.collection("users").document(ownerId).collection("menus").document(menu.getId()).set(menuData)
                  .addOnSuccessListener(aVoid -> {
                      Toast.makeText(this, "Menu diperbarui!", Toast.LENGTH_SHORT).show();
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
                    filterMenus(); 
                });
    }
}
