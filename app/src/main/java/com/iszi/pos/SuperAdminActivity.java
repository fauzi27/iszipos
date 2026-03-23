package com.iszi.pos;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuperAdminActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView tvMetricToko, tvMetricStaff, tvMetricDocs;
    private EditText inputSearchClient;
    private RecyclerView rvClients;

    private ClientAdapter adapter;
    private List<ClientModel> masterList;
    private List<ClientModel> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_superadmin);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        
        fetchAllData();
    }

    private void initViews() {
        tvMetricToko = findViewById(R.id.tvMetricToko);
        tvMetricStaff = findViewById(R.id.tvMetricStaff);
        tvMetricDocs = findViewById(R.id.tvMetricDocs);
        inputSearchClient = findViewById(R.id.inputSearchClient);
        rvClients = findViewById(R.id.rvClients);
    }

    private void setupRecyclerView() {
        masterList = new ArrayList<>();
        filteredList = new ArrayList<>();
        
        adapter = new ClientAdapter(filteredList, new ClientAdapter.OnGodModeListener() {
            @Override public void onTogglePlan(ClientModel client) { handleTogglePlan(client); }
            @Override public void onInjectData(ClientModel client) { handleInjectData(client); }
            @Override public void onEditLimit(ClientModel client) { Toast.makeText(SuperAdminActivity.this, "Fitur Atur Limit segera hadir!", Toast.LENGTH_SHORT).show(); }
            @Override public void onToggleSuspend(ClientModel client) { handleToggleSuspend(client); }
            @Override public void onDeleteClient(ClientModel client) { handleDeleteClient(client); }
        });

        rvClients.setLayoutManager(new LinearLayoutManager(this));
        rvClients.setAdapter(adapter);
    }

    private void setupListeners() {
        findViewById(R.id.btnLogoutAdmin).setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(SuperAdminActivity.this, LoginActivity.class));
            finish();
        });

        findViewById(R.id.btnRefreshAdmin).setOnClickListener(v -> {
            Toast.makeText(this, "Menyinkronkan data...", Toast.LENGTH_SHORT).show();
            fetchAllData();
        });

        inputSearchClient.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchAllData() {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                masterList.clear();
                int staffCount = 0;

                for (DocumentSnapshot doc : task.getResult()) {
                    String ownerId = doc.getString("ownerId");
                    
                    // Pisahkan Owner dan Kasir (Staff)
                    if (ownerId != null && !ownerId.isEmpty()) {
                        staffCount++;
                    } else {
                        ClientModel client = doc.toObject(ClientModel.class);
                        if (client != null) {
                            client.setId(doc.getId());
                            masterList.add(client);
                        }
                    }
                }

                // Update Metrik
                tvMetricToko.setText(String.valueOf(masterList.size()));
                tvMetricStaff.setText(String.valueOf(staffCount));
                tvMetricDocs.setText("Aman"); // Simplified

                applyFilters();
            } else {
                Toast.makeText(this, "Gagal mengambil data klien.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        filteredList.clear();
        String query = inputSearchClient.getText().toString().toLowerCase().trim();

        for (ClientModel c : masterList) {
            String name = c.getShopName() != null ? c.getShopName() : c.getName();
            boolean matchName = name != null && name.toLowerCase().contains(query);
            boolean matchEmail = c.getEmail() != null && c.getEmail().toLowerCase().contains(query);

            if (matchName || matchEmail || query.isEmpty()) {
                filteredList.add(c);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // === AKSI GOD MODE ===

    private void handleTogglePlan(ClientModel client) {
        boolean isPremium = "premium".equals(client.getPlan());
        String newPlan = isPremium ? "free" : "premium";
        String actionText = isPremium ? "Turunkan ke Akun FREE?" : "Upgrade ke ISZI PRO (Premium)?";

        new AlertDialog.Builder(this)
            .setTitle("Ubah Paket")
            .setMessage(actionText)
            .setPositiveButton("Ya", (dialog, which) -> {
                db.collection("users").document(client.getId()).update("plan", newPlan)
                    .addOnSuccessListener(aVoid -> fetchAllData());
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void handleToggleSuspend(ClientModel client) {
        boolean isSuspended = client.isSuspended();
        String actionText = isSuspended ? "Aktifkan Kembali Toko ini?" : "Blokir (Suspend) Toko ini?";

        new AlertDialog.Builder(this)
            .setTitle("Ubah Status")
            .setMessage(actionText)
            .setPositiveButton("Ya", (dialog, which) -> {
                db.collection("users").document(client.getId()).update("isSuspended", !isSuspended)
                    .addOnSuccessListener(aVoid -> fetchAllData());
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void handleInjectData(ClientModel client) {
        new AlertDialog.Builder(this)
            .setTitle("Suntik Starter Kit?")
            .setMessage("Toko ini akan diisi dengan kategori dan menu dasar (Nasi Goreng, Es Teh, dll).")
            .setPositiveButton("Suntik Sekarang", (dialog, which) -> executeInjectData(client.getId()))
            .setNegativeButton("Batal", null)
            .show();
    }

    private void executeInjectData(String ownerId) {
        WriteBatch batch = db.batch();

        // Suntik Kategori
        String[] cats = {"Makanan", "Minuman", "Camilan"};
        for (String c : cats) {
            Map<String, Object> catData = new HashMap<>();
            catData.put("name", c);
            batch.set(db.collection("users").document(ownerId).collection("categories").document(), catData);
        }

        // Suntik Menu
        String[][] menus = {
            {"Nasi Goreng Spesial", "15000", "makanan"},
            {"Mie Goreng Telur", "12000", "makanan"},
            {"Es Teh Manis", "5000", "minuman"}
        };

        for (String[] m : menus) {
            Map<String, Object> menuData = new HashMap<>();
            menuData.put("name", m[0]);
            menuData.put("price", Integer.parseInt(m[1]));
            menuData.put("category", m[2]);
            menuData.put("stock", 50);
            batch.set(db.collection("users").document(ownerId).collection("menus").document(), menuData);
        }

        batch.commit()
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Data Starter Kit berhasil disuntikkan!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void handleDeleteClient(ClientModel client) {
        new AlertDialog.Builder(this)
            .setTitle("HAPUS PERMANEN?")
            .setMessage("Apakah Anda yakin ingin membumihanguskan toko ini? Seluruh data akan lenyap.")
            .setPositiveButton("Ya, Hapus", (dialog, which) -> {
                db.collection("users").document(client.getId()).delete()
                    .addOnSuccessListener(aVoid -> fetchAllData());
            })
            .setNegativeButton("Batal", null)
            .show();
    }
}
