package com.iszi.pos;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Pastikan nama file XML kamu benar-benar 'main.xml' atau 'activity_main.xml'
        // Jika namanya activity_main, ganti R.layout.main menjadi R.layout.activity_main
        setContentView(R.layout.main);

        // --- KERANGKA WEBVIEW UNTUK KASIR ---
        // (Pastikan kamu menambahkan elemen WebView dengan id 'webView' di dalam file XML-nya nanti)
        webView = findViewById(R.id.webView);
        
        if (webView != null) {
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true); // Wajib agar React/Vite bisa jalan
            webSettings.setDomStorageEnabled(true); // Wajib untuk local storage web
            
            // Agar web tidak terbuka di aplikasi Google Chrome bawaan HP
            webView.setWebViewClient(new WebViewClient()); 

            // Nanti kita arahkan ke file web kasirmu di folder assets
            // webView.loadUrl("file:///android_asset/index.html"); 
        }
    }
    
    // Agar tombol 'Back' di HP kembali ke halaman web sebelumnya, bukan langsung keluar aplikasi
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
