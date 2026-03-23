package com.iszi.pos;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private boolean isLoginMode = true;
    private boolean isPasswordVisible = false;

    private TextView tvAuthTitle, tvToggleText, btnToggleMode;
    private LinearLayout boxRegisterOnly;
    private EditText inputBusinessName, inputAddress, inputId, inputPassword;
    private MaterialButton btnSubmit;
    private ImageButton btnTogglePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Jika sudah login, langsung lompat ke Lobby
        if (auth.getCurrentUser() != null) {
            goToLobby();
            return;
        }

        initViews();
        setupListeners();
        updateUI();
    }

    private void initViews() {
        tvAuthTitle = findViewById(R.id.tvAuthTitle);
        tvToggleText = findViewById(R.id.tvToggleText);
        btnToggleMode = findViewById(R.id.btnToggleMode);
        boxRegisterOnly = findViewById(R.id.boxRegisterOnly);
        inputBusinessName = findViewById(R.id.inputBusinessName);
        inputAddress = findViewById(R.id.inputAddress);
        inputId = findViewById(R.id.inputId);
        inputPassword = findViewById(R.id.inputPassword);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
    }

    private void setupListeners() {
        btnToggleMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUI();
        });

        btnTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                inputPassword.setInputType(InputType.TYPE_CLASS_TEXT); // Tampilkan
            } else {
                inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); // Sembunyikan
            }
            inputPassword.setSelection(inputPassword.getText().length());
        });

        btnSubmit.setOnClickListener(v -> {
            if (isLoginMode) performLogin();
            else performRegister();
        });
    }

    private void updateUI() {
        if (isLoginMode) {
            tvAuthTitle.setText("Masuk ke ISZI");
            boxRegisterOnly.setVisibility(View.GONE);
            inputId.setHint("Masukkan ID Kasir atau Email");
            btnSubmit.setText("Masuk");
            tvToggleText.setText("Belum punya akun? ");
            btnToggleMode.setText("Daftar Gratis");
        } else {
            tvAuthTitle.setText("Daftar Toko Baru");
            boxRegisterOnly.setVisibility(View.VISIBLE);
            inputId.setHint("Buat ID Kasir (Tanpa spasi)");
            btnSubmit.setText("Daftar Sekarang");
            tvToggleText.setText("Sudah punya akun? ");
            btnToggleMode.setText("Masuk di sini");
        }
    }

    private void performLogin() {
        String idOrEmail = inputId.getText().toString().trim().toLowerCase();
        String password = inputPassword.getText().toString().trim();

        if (idOrEmail.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Isi ID/Email dan password");
            return;
        }

        if (!idOrEmail.contains("@")) idOrEmail = idOrEmail + "@sahabatusahamu.com";

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Memproses...");

        auth.signInWithEmailAndPassword(idOrEmail, password)
            .addOnSuccessListener(authResult -> goToLobby())
            .addOnFailureListener(e -> {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Masuk");
                handleAuthError(e);
            });
    }

    private void performRegister() {
        String name = inputBusinessName.getText().toString().trim();
        String address = inputAddress.getText().toString().trim();
        String idOrEmail = inputId.getText().toString().trim().toLowerCase();
        String password = inputPassword.getText().toString().trim();

        if (name.isEmpty() || idOrEmail.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Data usaha dan login wajib diisi");
            return;
        }

        if (!idOrEmail.contains("@")) idOrEmail = idOrEmail + "@sahabatusahamu.com";

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Mendaftarkan...");

        String finalEmail = idOrEmail;
        auth.createUserWithEmailAndPassword(finalEmail, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user != null) {
                    // Simpan data toko ke Firestore
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", name);
                    userData.put("shopName", name);
                    userData.put("address", address);
                    userData.put("shopAddress", address);
                    userData.put("email", finalEmail);
                    userData.put("role", "owner");
                    userData.put("joinedAt", System.currentTimeMillis());
                    userData.put("maxTransactions", 0);
                    userData.put("maxMenus", 0);
                    userData.put("plan", "free"); // Sesuai permintaan: default free

                    db.collection("users").document(user.getUid()).set(userData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Akun dibuat! Selamat datang.", Toast.LENGTH_SHORT).show();
                            goToLobby();
                        });
                }
            })
            .addOnFailureListener(e -> {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Daftar Sekarang");
                handleAuthError(e);
            });
    }

    private void handleAuthError(Exception e) {
        String msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("password")) msg = "Password salah atau terlalu lemah.";
            if (msg.contains("record")) msg = "Akun tidak ditemukan.";
            if (msg.contains("already in use") || msg.contains("sudah digunakan")) msg = "Email/ID sudah digunakan orang lain.";
        }
        showAlert("Gagal", msg);
    }

    private void showAlert(String title, String message) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }

    private void goToLobby() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
