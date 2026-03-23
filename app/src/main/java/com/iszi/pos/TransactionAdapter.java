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
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    public TransactionAdapter(List<TransactionModel> txList) {
        this.txList = txList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionModel tx = txList.get(position);
        
        holder.tvTxBuyer.setText(tx.getBuyer() != null ? tx.getBuyer() : "Umum");
        holder.tvTxDate.setText(tx.getDate());
        holder.tvTxMethod.setText(tx.getMethod() != null ? tx.getMethod() : "TUNAI");
        holder.tvTxTotal.setText(formatRupiah.format(tx.getTotal()).replace("Rp", "Rp "));

        // Reset Styling (Penting untuk RecyclerView agar tidak bocor ke baris lain)
        holder.tvTxBuyer.setPaintFlags(holder.tvTxBuyer.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        holder.tvTxTotal.setPaintFlags(holder.tvTxTotal.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        holder.badgeHutang.setVisibility(View.GONE);
        holder.badgeSync.setText("✓ UPLOADED");
        holder.badgeSync.setTextColor(Color.parseColor("#166534"));
        holder.badgeSync.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DCFCE3")));

        boolean isRefunded = "REFUNDED".equals(tx.getStatus());
        boolean hasHutang = tx.getRemaining() > 0;

        if (tx.isPendingSync()) {
            holder.badgeSync.setText("⏳ BLM UPLOAD");
            holder.badgeSync.setTextColor(Color.parseColor("#EF4444"));
            holder.badgeSync.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
        }

        if (isRefunded) {
            holder.viewStatusColor.setBackgroundColor(Color.parseColor("#9CA3AF")); // Abu-abu
            holder.tvTxBuyer.setPaintFlags(holder.tvTxBuyer.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTxTotal.setPaintFlags(holder.tvTxTotal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.badgeSync.setText("BATAL");
            holder.badgeSync.setTextColor(Color.parseColor("#4B5563"));
            holder.badgeSync.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E5E7EB")));
        } else if (hasHutang) {
            holder.viewStatusColor.setBackgroundColor(Color.parseColor("#F59E0B")); // Orange
            holder.badgeHutang.setVisibility(View.VISIBLE);
        } else {
            holder.viewStatusColor.setBackgroundColor(Color.parseColor("#22C55E")); // Hijau Lunas
        }
    }

    @Override
    public int getItemCount() { return txList.size(); }

    public void updateList(List<TransactionModel> newList) {
        txList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTxBuyer, tvTxDate, tvTxTotal, tvTxMethod, badgeSync, badgeHutang;
        View viewStatusColor;

        public ViewHolder(View itemView) {
            super(itemView);
            tvTxBuyer = itemView.findViewById(R.id.tvTxBuyer);
            tvTxDate = itemView.findViewById(R.id.tvTxDate);
            tvTxTotal = itemView.findViewById(R.id.tvTxTotal);
            tvTxMethod = itemView.findViewById(R.id.tvTxMethod);
            badgeSync = itemView.findViewById(R.id.badgeSync);
            badgeHutang = itemView.findViewById(R.id.badgeHutang);
            viewStatusColor = itemView.findViewById(R.id.viewStatusColor);
        }
    }
}
