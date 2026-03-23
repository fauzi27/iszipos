package com.iszi.pos;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View; // INI YANG HILANG!
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import java.util.Collections;
import java.util.List;

public class StockActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String ownerId;

    private RecyclerView rvStock;
    private StockAdapter adapter;
    private List<MenuModel> masterList = new ArrayList<>();
    private List<MenuModel> filteredList = new ArrayList<>();

    private EditText inputSearchStock;
    private LinearLayout btnSort;
    private TextView tvSortLabel;
    private String sortMode = "default";

    private String currentInputMode = "add";
    private MenuModel selectedMenu;
    private int predictedTotal = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) ownerId = currentUser.getUid();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadMenusFromFirebase();
    }

    private void initViews() {
        rvStock = findViewById(R.id.rvStock);
        inputSearchStock = findViewById(R.id.inputSearchStock);
        btnSort = findViewById(R.id.btnSort);
        tvSortLabel = findViewById(R.id.tvSortLabel);
    }

    private void setupRecyclerView() {
        adapter = new StockAdapter(filteredList, new StockAdapter.OnStockClickListener() {
            @Override public void onMinClick(MenuModel menu) { updateStockFast(menu, -1); }
            @Override public void onPlusClick(MenuModel menu) { updateStockFast(menu, 1); }
            @Override public void onNumberClick(MenuModel menu) { showSmartInputDialog(menu); }
        });
        rvStock.setLayoutManager(new LinearLayoutManager(this));
        rvStock.setAdapter(adapter);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSort.setOnClickListener(v -> {
            if (sortMode.equals("default")) { sortMode = "stock_low"; tvSortLabel.setText("Stok Menipis"); }
            else if (sortMode.equals("stock_low")) { sortMode = "stock_high"; tvSortLabel.setText("Stok Banyak"); }
            else { sortMode = "default"; tvSortLabel.setText("Default"); }
            applyFilters();
        });

        inputSearchStock.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadMenusFromFirebase() {
        if (ownerId == null) return;
        db.collection("users").document(ownerId).collection("menus")
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) return;
                masterList.clear();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        MenuModel m = doc.toObject(MenuModel.class);
                        m.setId(doc.getId());
                        masterList.add(m);
                    }
                }
                applyFilters();
            });
    }

    private void applyFilters() {
        filteredList.clear();
        String query = inputSearchStock.getText().toString().toLowerCase().trim();

        for (MenuModel m : masterList) {
            if (m.getName().toLowerCase().contains(query)) {
                filteredList.add(m);
            }
        }

        if (sortMode.equals("stock_low")) {
            Collections.sort(filteredList, (a, b) -> Integer.compare(a.getStock(), b.getStock()));
        } else if (sortMode.equals("stock_high")) {
            Collections.sort(filteredList, (a, b) -> Integer.compare(b.getStock(), a.getStock()));
        }
        adapter.notifyDataSetChanged();
    }

    private void updateStockFast(MenuModel menu, int change) {
        int newStock = menu.getStock() + change;
        if (newStock < 0) return;
        saveStockToFirebase(menu.getId(), newStock);
    }

    private void showSmartInputDialog(MenuModel menu) {
        selectedMenu = menu;
        currentInputMode = "add";
        predictedTotal = menu.getStock();

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_stock_input);
        
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        TextView tvModalTitle = dialog.findViewById(R.id.tvModalTitle);
        TextView tvPreviewAwal = dialog.findViewById(R.id.tvPreviewAwal);
        TextView tvPreviewAkhir = dialog.findViewById(R.id.tvPreviewAkhir);
        EditText inputStockNumber = dialog.findViewById(R.id.inputStockNumber);
        
        MaterialButton btnModeAdd = dialog.findViewById(R.id.btnModeAdd);
        MaterialButton btnModeSub = dialog.findViewById(R.id.btnModeSub);
        MaterialButton btnModeSet = dialog.findViewById(R.id.btnModeSet);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSaveStockModal);

        tvModalTitle.setText("Update Stok: " + menu.getName());
        tvPreviewAwal.setText(String.valueOf(menu.getStock()));
        tvPreviewAkhir.setText(String.valueOf(predictedTotal));

        Runnable updatePrediction = () -> {
            String valStr = inputStockNumber.getText().toString();
            int parsedVal = valStr.isEmpty() ? 0 : Integer.parseInt(valStr);

            if (currentInputMode.equals("add")) predictedTotal = menu.getStock() + parsedVal;
            else if (currentInputMode.equals("sub")) predictedTotal = menu.getStock() - parsedVal;
            else if (currentInputMode.equals("set")) predictedTotal = parsedVal;

            tvPreviewAkhir.setText(String.valueOf(predictedTotal));
            
            if (predictedTotal < 0) {
                tvPreviewAkhir.setTextColor(Color.parseColor("#EF4444"));
                btnSave.setEnabled(false);
            } else {
                tvPreviewAkhir.setTextColor(Color.WHITE);
                btnSave.setEnabled(true);
            }
        };

        View.OnClickListener modeListener = v -> {
            btnModeAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0F172A")));
            btnModeAdd.setTextColor(Color.parseColor("#6B7280"));
            btnModeSub.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0F172A")));
            btnModeSub.setTextColor(Color.parseColor("#6B7280"));
            btnModeSet.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0F172A")));
            btnModeSet.setTextColor(Color.parseColor("#6B7280"));

            if (v.getId() == R.id.btnModeAdd) {
                currentInputMode = "add";
                btnModeAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#22C55E")));
                btnModeAdd.setTextColor(Color.WHITE);
            } else if (v.getId() == R.id.btnModeSub) {
                currentInputMode = "sub";
                btnModeSub.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444")));
                btnModeSub.setTextColor(Color.WHITE);
            } else if (v.getId() == R.id.btnModeSet) {
                currentInputMode = "set";
                btnModeSet.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3B82F6")));
                btnModeSet.setTextColor(Color.WHITE);
            }
            updatePrediction.run();
        };

        btnModeAdd.setOnClickListener(modeListener);
        btnModeSub.setOnClickListener(modeListener);
        btnModeSet.setOnClickListener(modeListener);

        inputStockNumber.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updatePrediction.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialog.findViewById(R.id.btnCloseModal).setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            if (predictedTotal >= 0) {
                saveStockToFirebase(menu.getId(), predictedTotal);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void saveStockToFirebase(String menuId, int finalStock) {
        if (ownerId == null) return;
        db.collection("users").document(ownerId).collection("menus").document(menuId)
            .update("stock", finalStock)
            .addOnFailureListener(e -> Toast.makeText(this, "Gagal update stok", Toast.LENGTH_SHORT).show());
    }
}
