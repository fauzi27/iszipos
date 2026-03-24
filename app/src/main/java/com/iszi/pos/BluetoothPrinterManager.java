package com.iszi.pos;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothPrinterManager {
    
    // UUID Standar Internasional untuk Printer Bluetooth (Serial Port Profile)
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private SharedPreferences prefs;

    public BluetoothPrinterManager(Context context) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Memori lokal untuk menyimpan printer terakhir yang dikoneksikan
        prefs = context.getSharedPreferences("PrinterPrefs", Context.MODE_PRIVATE);
    }

    public void savePrinter(String macAddress, String name) {
        prefs.edit().putString("PRINTER_MAC", macAddress).putString("PRINTER_NAME", name).apply();
    }

    public String getSavedPrinterMac() {
        return prefs.getString("PRINTER_MAC", null);
    }
    
    public String getSavedPrinterName() {
        return prefs.getString("PRINTER_NAME", "Belum terhubung ke printer");
    }

    @SuppressLint("MissingPermission")
    public boolean connect() {
        String mac = getSavedPrinterMac();
        if (mac == null || bluetoothAdapter == null) return false;

        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mac);
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
            bluetoothAdapter.cancelDiscovery(); // Hentikan pencarian agar koneksi lebih cepat
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            return true;
        } catch (Exception e) {
            disconnect();
            return false;
        }
    }

    public void disconnect() {
        try {
            if (outputStream != null) {
                outputStream.flush();
                outputStream.close();
            }
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Fungsi untuk mengirim teks biasa
    public void printText(String text) {
        if (outputStream == null) return;
        try {
            // GBK atau ASCII standar untuk printer thermal
            outputStream.write(text.getBytes("GBK")); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Fungsi untuk mengirim kode rahasia printer (ESC/POS)
    public void printBytes(byte[] bytes) {
        if (outputStream == null) return;
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
