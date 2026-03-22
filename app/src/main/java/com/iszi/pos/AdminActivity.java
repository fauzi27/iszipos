package com.iszi.pos;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    private RecyclerView rvMenus;
    private MenuAdapter menuAdapter;
    private List<MenuModel> menuList; // Daftar asli dari Firebase
    private List<MenuModel> filteredList; // Daftar untuk hasil pencarian

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

        // 1. Inisialisasi Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // Asumsi sementara: yang login adalah Owner (menggunakan UID sendiri)
            // Nanti bisa disesuaikan jika Kasir yang login untuk mengambil ownerId-nya
            ownerId = currentUser.getUid(); 
        }

        // 2. Hubungkan UI
        initViews();
        
        // 3. Siapkan Wadah Daftar (RecyclerView)
        setupRecyclerView();
        
        // 4. Tarik Data dari Firestore
        loadMenus();
        
        // 5. Aktifkan Kolom Pencarian
        setupSearch();

        // 6. Aksi Tombol-tombol Utama
        btnBack.setOnClickListener(v -> finish()); // Kembali ke Lobby
        
        btnAddMenu.setOnClickListener(v -> {
            // Nanti kita buatkan Dialog Popup Form di sini
            Toast.makeText(this, "Modal Form Tambah Menu akan segera dibuat!", Toast.LENGTH_SHORT).show();
        });
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
                Toast.makeText(AdminActivity.this, "Fitur Edit: " + menu.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(MenuModel menu) {
                Toast.makeText(AdminActivity.this, "Fitur Hapus: " + menu.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        rvMenus.setLayoutManager(new LinearLayoutManager(this));
        rvMenus.setAdapter(menuAdapter);
    }

    private void loadMenus() {
        if (ownerId == null) return;

        // Ekuivalen dengan onSnapshot di React Native (Realtime Update)
        db.collection("users").document(ownerId).collection("menus")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(AdminActivity.this, "Gagal memuat data menu", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    menuList.clear(); // Bersihkan daftar lama
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            MenuModel menu = doc.toObject(MenuModel.class);
                            menu.setId(doc.getId()); // Ambil ID Dokumen Firebase-nya
                            menuList.add(menu);
                        }
                    }
                    
                    // Segarkan tampilan sesuai teks pencarian terakhir
                    filterMenus(inputSearch.getText().toString());
                });
    }

    private void setupSearch() {
        // Ini fungsinya persis seperti onChangeText={setSearchQuery} di React Native
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMenus(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterMenus(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(menuList); // Jika kosong, tampilkan semua
        } else {
            String query = text.toLowerCase();
            for (MenuModel menu : menuList) {
                if (menu.getName() != null && menu.getName().toLowerCase().contains(query)) {
                    filteredList.add(menu);
                }
            }
        }
        menuAdapter.notifyDataSetChanged(); // Suruh RecyclerView update layarnya
    }
}
