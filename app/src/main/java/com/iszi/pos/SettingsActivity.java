package com.iszi.pos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private EditText inputShopName, inputShopAddress, inputFooter, inputEmpName, inputEmpId, inputEmpPass;
    private SwitchCompat switchWatermark;
    private MaterialButton btnSaveProfile, btnAddEmployee, btnConnectPrinter, btnBackup;
    private ImageButton btnBack;
    private RecyclerView rvEmployees;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseAuth secondaryAuth; 
    private String currentUserUid;

    private List<EmployeeAdapter.EmployeeModel> employeeList = new ArrayList<>();
    private EmployeeAdapter adapter;

    private String pendingBackupContent = "";
    private ActivityResultLauncher<Intent> backupSaveLauncher;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", new Locale("id", "ID"));

    private BluetoothPrinterManager printerManager;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 🔥 MESIN TEMA DINAMIS DIPANGGIL DI SINI 🔥
        ThemeManager.setCustomTheme(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) currentUserUid = user.getUid();

        printerManager = new BluetoothPrinterManager(this);

        initSecondaryAuth(); 
        setupFileSaver();
        setupPermissions(); 
        
        initViews();
        loadUserProfile();
        loadEmployees();
        setupListeners();
        
        updatePrinterButtonText();
    }

    private void updatePrinterButtonText() {
        if (btnConnectPrinter != null) {
            String savedName = printerManager.getSavedPrinterName();
            btnConnectPrinter.setText("Printer: " + savedName);
        }
    }

    private void setupPermissions() {
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean allGranted = true;
            for (Boolean granted : result.values()) { if (!granted) allGranted = false; }
            if (allGranted) showPairedPrinters();
            else Toast.makeText(this, "Izin Bluetooth ditolak! Tidak bisa mencari printer.", Toast.LENGTH_SHORT).show();
        });
    }

    private void initSecondaryAuth() {
        try {
            FirebaseOptions options = FirebaseApp.getInstance().getOptions();
            FirebaseApp secondaryApp;
            try { secondaryApp = FirebaseApp.initializeApp(this, options, "SecondaryApp"); } 
            catch (IllegalStateException e) { secondaryApp = FirebaseApp.getInstance("SecondaryApp"); }
            secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupFileSaver() {
        backupSaveLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                try {
                    OutputStream os = getContentResolver().openOutputStream(result.getData().getData());
                    os.write(pendingBackupContent.getBytes());
                    os.close();
                    Toast.makeText(this, "Backup Berhasil Disimpan!", Toast.LENGTH_LONG).show();
                } catch (Exception e) { Toast.makeText(this, "Gagal backup: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
            }
        });
    }

    private void initViews() {
        inputShopName = findViewById(R.id.inputShopName);
        inputShopAddress = findViewById(R.id.inputShopAddress);
        inputFooter = findViewById(R.id.inputFooter);
        switchWatermark = findViewById(R.id.switchWatermark);
        
        inputEmpName = findViewById(R.id.inputEmpName);
        inputEmpId = findViewById(R.id.inputEmpId);
        inputEmpPass = findViewById(R.id.inputEmpPass);
        
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnAddEmployee = findViewById(R.id.btnAddEmployee);
        btnConnectPrinter = findViewById(R.id.btnConnectPrinter);
        btnBackup = findViewById(R.id.btnBackup);
        btnBack = findViewById(R.id.btnBack);
        
        rvEmployees = findViewById(R.id.rvEmployees);
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new EmployeeAdapter(employeeList, new EmployeeAdapter.EmployeeActionListener() {
            @Override public void onUpdateAccess(String empId, String accessKey, boolean newValue) { db.collection("users").document(empId).update("accessRights." + accessKey, newValue); }
            @Override public void onDelete(String empId, String empName) { 
                new AlertDialog.Builder(SettingsActivity.this).setTitle("Hapus Karyawan?").setMessage("Akses " + empName + " akan dicabut!").setPositiveButton("Hapus", (d, w) -> db.collection("users").document(empId).delete()).setNegativeButton("Batal", null).show(); 
            }
        });
        rvEmployees.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnAddEmployee.setOnClickListener(v -> addEmployee());
        btnBackup.setOnClickListener(v -> executeFullBackup());
        btnConnectPrinter.setOnClickListener(v -> checkBluetoothAndShowPrinters());
    }

    private void checkBluetoothAndShowPrinters() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN});
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
                return;
            }
        }
        showPairedPrinters();
    }

    @SuppressLint("MissingPermission")
    private void showPairedPrinters() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) { Toast.makeText(this, "HP ini tidak memiliki Bluetooth", Toast.LENGTH_SHORT).show(); return; }
        if (!bluetoothAdapter.isEnabled()) { Toast.makeText(this, "Mohon nyalakan Bluetooth HP Anda terlebih dahulu!", Toast.LENGTH_LONG).show(); return; }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices == null || pairedDevices.isEmpty()) {
            Toast.makeText(this, "Tidak ada perangkat Bluetooth yang di-pairing. Pairing dulu printer Anda di Pengaturan HP.", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> listNamas = new ArrayList<>();
        final List<BluetoothDevice> listDevices = new ArrayList<>();

        for (BluetoothDevice device : pairedDevices) {
            listNamas.add(device.getName() + "\n" + device.getAddress());
            listDevices.add(device);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pilih Printer Thermal");
        
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice, listNamas);
        builder.setAdapter(arrayAdapter, (dialog, which) -> {
            BluetoothDevice selectedDevice = listDevices.get(which);
            printerManager.savePrinter(selectedDevice.getAddress(), selectedDevice.getName());
            updatePrinterButtonText();
            Toast.makeText(this, "Printer " + selectedDevice.getName() + " berhasil dipilih!", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("Batal", null);
        builder.show();
    }

    private void loadUserProfile() {
        if (currentUserUid == null) return;
        db.collection("users").document(currentUserUid).addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                if (snapshot.contains("shopName")) inputShopName.setText(snapshot.getString("shopName"));
                if (snapshot.contains("shopAddress")) inputShopAddress.setText(snapshot.getString("shopAddress"));
                if (snapshot.contains("receiptFooter")) inputFooter.setText(snapshot.getString("receiptFooter"));
                if (snapshot.contains("removeWatermark")) switchWatermark.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("removeWatermark")));
            }
        });
    }

    private void saveProfile() {
        if (currentUserUid == null) return;
        String name = inputShopName.getText().toString().trim();
        String address = inputShopAddress.getText().toString().trim();
        if (name.isEmpty() || address.isEmpty()) { Toast.makeText(this, "Data tidak boleh kosong!", Toast.LENGTH_SHORT).show(); return; }

        Map<String, Object> updates = new HashMap<>();
        updates.put("shopName", name); updates.put("name", name); 
        updates.put("shopAddress", address); updates.put("address", address); 
        updates.put("receiptFooter", inputFooter.getText().toString().trim());
        updates.put("removeWatermark", switchWatermark.isChecked());

        db.collection("users").document(currentUserUid).update(updates).addOnSuccessListener(aVoid -> Toast.makeText(this, "Tersimpan!", Toast.LENGTH_SHORT).show());
    }

    private void loadEmployees() {
        if (currentUserUid == null) return;
        db.collection("users").whereEqualTo("ownerId", currentUserUid).addSnapshotListener((snapshot, e) -> {
            if (snapshot != null) {
                employeeList.clear();
                for (QueryDocumentSnapshot doc : snapshot) {
                    Map<String, Boolean> access = (Map<String, Boolean>) doc.get("accessRights");
                    employeeList.add(new EmployeeAdapter.EmployeeModel(doc.getId(), doc.getString("name"), doc.getString("email"), access));
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void addEmployee() {
        if (secondaryAuth == null || currentUserUid == null) return;
        String name = inputEmpName.getText().toString().trim();
        String email = inputEmpId.getText().toString().trim().toLowerCase();
        String pass = inputEmpPass.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || pass.length() < 6) { Toast.makeText(this, "Lengkapi data (Password min 6)", Toast.LENGTH_SHORT).show(); return; }
        if (!email.contains("@")) email = email + "@sahabatusahamu.com"; 

        Toast.makeText(this, "Mendaftarkan Karyawan...", Toast.LENGTH_SHORT).show();
        final String finalEmail = email;

        secondaryAuth.createUserWithEmailAndPassword(email, pass).addOnSuccessListener(authResult -> {
            String newUid = authResult.getUser().getUid();
            Map<String, Boolean> defaultAccess = new HashMap<>();
            defaultAccess.put("cashier", true); defaultAccess.put("calculator", true);
            defaultAccess.put("report", false); defaultAccess.put("stock", false);
            defaultAccess.put("table", false); defaultAccess.put("admin", false); defaultAccess.put("settings", false);

            Map<String, Object> empData = new HashMap<>();
            empData.put("name", name); empData.put("email", finalEmail);
            empData.put("role", "kasir"); empData.put("ownerId", currentUserUid);
            empData.put("accessRights", defaultAccess);

            db.collection("users").document(newUid).set(empData).addOnSuccessListener(aVoid -> {
                inputEmpName.setText(""); inputEmpId.setText(""); inputEmpPass.setText("");
                secondaryAuth.signOut(); 
                Toast.makeText(this, "Kasir Berhasil Didaftarkan!", Toast.LENGTH_LONG).show();
            });
        }).addOnFailureListener(e -> Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void executeFullBackup() {
        if (currentUserUid == null) return;
        Toast.makeText(this, "Mengumpulkan data...", Toast.LENGTH_SHORT).show();
        StringBuilder csv = new StringBuilder();
        db.collection("users").document(currentUserUid).collection("menus").get().addOnSuccessListener(menuSnap -> {
            csv.append("--- DATA MENU & STOK ---\nID System,Nama Menu,Kategori,Harga (Rp),Sisa Stok\n");
            for (QueryDocumentSnapshot doc : menuSnap) csv.append(doc.getId()).append(",").append(doc.getString("name")).append(",UMUM,").append(doc.contains("price")?doc.getLong("price"):0).append(",").append(doc.contains("stock")?doc.getLong("stock"):0).append("\n");
            csv.append("\n\n");
            db.collection("users").document(currentUserUid).collection("transactions").get().addOnSuccessListener(txSnap -> {
                csv.append("--- DATA RIWAYAT TRANSAKSI ---\nTanggal,Kasir,Pelanggan,Total Belanja (Rp),Dibayar (Rp),Sisa Hutang (Rp),Metode,Status\n");
                for (QueryDocumentSnapshot doc : txSnap) {
                    TransactionModel tx = doc.toObject(TransactionModel.class);
                    String status = "REFUNDED".equals(tx.getStatus()) ? "BATAL" : (tx.getRemaining() > 0 ? "HUTANG" : "LUNAS");
                    csv.append(tx.getDate()!=null?tx.getDate().replace(",",""):sdf.format(new Date(tx.getTimestamp()))).append(",").append(tx.getOperatorName()!=null?tx.getOperatorName():"Admin").append(",").append(tx.getBuyer()!=null?tx.getBuyer():"Umum").append(",").append(tx.getTotal()).append(",").append(tx.getPaid()).append(",").append(tx.getRemaining()).append(",").append(tx.getMethod()!=null?tx.getMethod():"TUNAI").append(",").append(status).append("\n");
                }
                pendingBackupContent = csv.toString();
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("text/comma-separated-values");
                intent.putExtra(Intent.EXTRA_TITLE, "Backup_Database_" + System.currentTimeMillis() + ".csv");
                backupSaveLauncher.launch(intent);
            });
        });
    }
}
