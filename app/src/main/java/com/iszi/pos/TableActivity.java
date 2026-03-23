package com.iszi.pos;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TableActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String ownerId;

    private RecyclerView rvTable;
    private SwipeRefreshLayout swipeRefreshTable;
    private EditText inputSearchTable;

    private TableAdapter adapter;
    private List<TransactionModel> masterList;
    private List<TransactionModel> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) ownerId = currentUser.getUid();

        initViews();
        setupRecyclerView();
        setupListeners();
        
        // Panggil data pertama kali
        fetchTransactions();
    }

    private void initViews() {
        rvTable = findViewById(R.id.rvTable);
        swipeRefreshTable = findViewById(R.id.swipeRefreshTable);
        inputSearchTable = findViewById(R.id.inputSearchTable);
    }

    private void setupRecyclerView() {
        masterList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new TableAdapter(filteredList);
        rvTable.setLayoutManager(new LinearLayoutManager(this));
        rvTable.setAdapter(adapter);
    }

    private void setupListeners() {
        // Tombol Kembali
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Tombol Export
        findViewById(R.id.btnExportAudit).setOnClickListener(v -> {
            Toast.makeText(this, "Fitur Export PDF/Excel (Unlocked) sedang disiapkan mesinnya!", Toast.LENGTH_LONG).show();
        });

        // Tarik Layar ke Bawah (Refresh)
        swipeRefreshTable.setOnRefreshListener(() -> fetchTransactions());

        // Kolom Pencarian Cerdas
        inputSearchTable.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchTransactions() {
        if (ownerId == null) {
            swipeRefreshTable.setRefreshing(false);
            return;
        }
        
        // Tampilkan animasi loading
        swipeRefreshTable.setRefreshing(true);

        // Ambil 50 transaksi terbaru (Descending) agar tidak membebani memori HP
        db.collection("users").document(ownerId).collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnCompleteListener(task -> {
                swipeRefreshTable.setRefreshing(false);
                if (task.isSuccessful() && task.getResult() != null) {
                    masterList.clear();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        TransactionModel t = doc.toObject(TransactionModel.class);
                        t.setId(doc.getId());
                        masterList.add(t);
                    }
                    applyFilters();
                } else {
                    Toast.makeText(this, "Gagal memuat data buku besar.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void applyFilters() {
        filteredList.clear();
        String query = inputSearchTable.getText().toString().toLowerCase().trim();

        for (TransactionModel t : masterList) {
            boolean matchBuyer = t.getBuyer() != null && t.getBuyer().toLowerCase().contains(query);
            boolean matchKasir = t.getOperatorName() != null && t.getOperatorName().toLowerCase().contains(query);
            boolean matchStatus = t.getStatus() != null && t.getStatus().toLowerCase().contains(query);
            
            // Masukkan data jika cocok dengan filter
            if (matchBuyer || matchKasir || matchStatus || query.isEmpty()) {
                filteredList.add(t);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
