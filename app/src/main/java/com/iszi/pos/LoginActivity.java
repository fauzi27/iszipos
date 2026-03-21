package com.iszi.pos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

// 1. PERUBAHAN KE ANDROIDX:
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView txtToggleRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            goToLobby();
            return;
        }

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        txtToggleRegister = findViewById(R.id.txtToggleRegister);

        // Menggunakan lambda agar kode lebih bersih (opsional, tapi disarankan untuk Java 8+)
        btnLogin.setOnClickListener(v -> handleLogin());

        txtToggleRegister.setOnClickListener(v -> 
            Toast.makeText(LoginActivity.this, "Halaman Daftar belum dibuat", Toast.LENGTH_SHORT).show()
        );
    }

    private void handleLogin() {
        String rawInputId = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (rawInputId.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Isi ID/Email dan password");
            return;
        }

        String email = rawInputId.toLowerCase();
        if (!email.contains("@")) {
            email = email + "@sahabatusahamu.com";
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                setLoading(false);

                if (task.isSuccessful()) {
                    goToLobby();
                } else {
                    handleAuthError(task.getException());
                }
            });
    }

    private void handleAuthError(Exception exception) {
        String msg = exception != null ? exception.getMessage() : "Terjadi kesalahan";
        if (exception instanceof FirebaseAuthInvalidUserException) {
            msg = "Email/ID tidak terdaftar.";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            msg = "Password salah / Akun tidak ditemukan.";
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

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            btnLogin.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            btnLogin.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    private void goToLobby() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
