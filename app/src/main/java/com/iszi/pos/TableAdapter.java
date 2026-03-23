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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TableAdapter extends RecyclerView.Adapter<TableAdapter.ViewHolder> {

    private List<TransactionModel> txList;
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
    private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));

    public TableAdapter(List<TransactionModel> txList) { 
        this.txList = txList; 
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_table_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionModel tx = txList.get(position);

        // Generate Invoice ID buatan berdasarkan timestamp
        long time = tx.getTimestamp() > 0 ? tx.getTimestamp() : System.currentTimeMillis();
        String invoiceId = "INV-" + time;
        holder.tvTableInvoice.setText(invoiceId.substring(0, 14));

        String dateStr = tx.getDate();
        if (dateStr == null || dateStr.isEmpty()) {
            dateStr = sdf.format(new Date(time));
        }
        holder.tvTableDate.setText(dateStr);

        holder.tvTableKasir.setText("Kasir: " + (tx.getOperatorName() != null ? tx.getOperatorName() : "Admin"));
        holder.tvTableBuyer.setText(tx.getBuyer() != null ? tx.getBuyer() : "Pelanggan Umum");
        holder.tvTableTotal.setText(formatRupiah.format(tx.getTotal()).replace("Rp", "Rp "));

        // Logika Status
        boolean isRefunded = "REFUNDED".equals(tx.getStatus());
        boolean hasHutang = tx.getRemaining() > 0;

        // Reset coretan agar tidak error bocor ke baris lain saat discroll
        holder.tvTableTotal.setPaintFlags(holder.tvTableTotal.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        holder.tvTableBuyer.setPaintFlags(holder.tvTableBuyer.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

        if (isRefunded) {
            holder.badgeTableStatus.setText("BATAL");
            holder.badgeTableStatus.setTextColor(Color.parseColor("#9CA3AF")); // Abu-abu
            holder.badgeTableStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#334155")));
            holder.tvTableTotal.setPaintFlags(holder.tvTableTotal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTableBuyer.setPaintFlags(holder.tvTableBuyer.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else if (hasHutang) {
            holder.badgeTableStatus.setText("HUTANG");
            holder.badgeTableStatus.setTextColor(Color.parseColor("#FCA5A5")); // Merah Muda
            holder.badgeTableStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#7F1D1D")));
        } else {
            holder.badgeTableStatus.setText("LUNAS");
            holder.badgeTableStatus.setTextColor(Color.parseColor("#34D399")); // Hijau
            holder.badgeTableStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#064E3B")));
        }
    }

    @Override
    public int getItemCount() { return txList.size(); }

    public void updateList(List<TransactionModel> newList) {
        txList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTableInvoice, tvTableDate, tvTableKasir, tvTableBuyer, tvTableTotal, badgeTableStatus;
        public ViewHolder(View itemView) {
            super(itemView);
            tvTableInvoice = itemView.findViewById(R.id.tvTableInvoice);
            tvTableDate = itemView.findViewById(R.id.tvTableDate);
            tvTableKasir = itemView.findViewById(R.id.tvTableKasir);
            tvTableBuyer = itemView.findViewById(R.id.tvTableBuyer);
            tvTableTotal = itemView.findViewById(R.id.tvTableTotal);
            badgeTableStatus = itemView.findViewById(R.id.badgeTableStatus);
        }
    }
}
