package com.iszi.pos;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<TransactionModel> txList;
    private OnTransactionClickListener listener;
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionModel tx);
    }

    public TransactionAdapter(List<TransactionModel> txList, OnTransactionClickListener listener) {
        this.txList = txList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.itemtransaction, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionModel tx = txList.get(position);
        
        boolean isRefunded = "REFUNDED".equals(tx.getStatus());
        boolean hasHutang = tx.getRemaining() > 0;

        holder.tvTxBuyer.setText(tx.getBuyer() != null ? tx.getBuyer() : "Umum");
        holder.tvTxDate.setText(tx.getDate());
        holder.tvTxTotal.setText(formatRupiah.format(tx.getTotal()).replace("Rp", "Rp "));
        holder.tvTxMethod.setText(tx.getMethod() != null ? tx.getMethod() : "TUNAI");

        // RESET STYLE (Karena RecyclerView menggunakan ulang view lama)
        holder.tvTxBuyer.setPaintFlags(holder.tvTxBuyer.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        holder.tvTxTotal.setPaintFlags(holder.tvTxTotal.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        holder.badgeHutang.setVisibility(View.GONE);
        holder.badgeSync.setText("UPLOADED");
        holder.badgeSync.setTextColor(Color.parseColor("#166534"));
        holder.badgeSync.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DCFCE3")));

        // LOGIKA WARNA STATUS
        if (isRefunded) {
            holder.viewStatusColor.setBackgroundColor(Color.parseColor("#9CA3AF")); // Abu-abu
            holder.tvTxBuyer.setPaintFlags(holder.tvTxBuyer.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTxTotal.setPaintFlags(holder.tvTxTotal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTxBuyer.setTextColor(Color.parseColor("#9CA3AF"));
            holder.tvTxTotal.setTextColor(Color.parseColor("#9CA3AF"));
            
            holder.badgeSync.setText("BATAL");
            holder.badgeSync.setTextColor(Color.parseColor("#94A3B8"));
            holder.badgeSync.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#334155")));
        } else if (hasHutang) {
            holder.viewStatusColor.setBackgroundColor(Color.parseColor("#F59E0B")); // Kuning/Oranye
            holder.badgeHutang.setVisibility(View.VISIBLE);
            holder.tvTxMethod.setTextColor(Color.parseColor("#F59E0B"));
            holder.tvTxBuyer.setTextColor(Color.WHITE);
            holder.tvTxTotal.setTextColor(Color.WHITE);
        } else {
            // LUNAS Normal
            if ("QRIS".equals(tx.getMethod())) {
                holder.viewStatusColor.setBackgroundColor(Color.parseColor("#3B82F6")); // Biru QRIS
            } else {
                holder.viewStatusColor.setBackgroundColor(Color.parseColor("#22C55E")); // Hijau Tunai
            }
            holder.tvTxBuyer.setTextColor(Color.WHITE);
            holder.tvTxTotal.setTextColor(Color.WHITE);
            holder.tvTxMethod.setTextColor(Color.parseColor("#9CA3AF"));
        }

        holder.itemView.setOnClickListener(v -> listener.onTransactionClick(tx));
    }

    @Override
    public int getItemCount() {
        return txList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View viewStatusColor;
        TextView tvTxBuyer, badgeSync, badgeHutang, tvTxDate, tvTxTotal, tvTxMethod;

        public ViewHolder(View itemView) {
            super(itemView);
            viewStatusColor = itemView.findViewById(R.id.viewStatusColor);
            tvTxBuyer = itemView.findViewById(R.id.tvTxBuyer);
            badgeSync = itemView.findViewById(R.id.badgeSync);
            badgeHutang = itemView.findViewById(R.id.badgeHutang);
            tvTxDate = itemView.findViewById(R.id.tvTxDate);
            tvTxTotal = itemView.findViewById(R.id.tvTxTotal);
            tvTxMethod = itemView.findViewById(R.id.tvTxMethod);
        }
    }
}
