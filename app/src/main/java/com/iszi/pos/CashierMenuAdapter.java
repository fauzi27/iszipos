package com.iszi.pos;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CashierMenuAdapter extends RecyclerView.Adapter<CashierMenuAdapter.ViewHolder> {
    private List<MenuModel> menuList;
    private OnMenuClickListener listener;
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    public interface OnMenuClickListener { void onMenuClick(MenuModel menu); }

    public CashierMenuAdapter(List<MenuModel> menuList, OnMenuClickListener listener) {
        this.menuList = menuList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuModel menu = menuList.get(position);
        holder.tvTitle.setText(menu.getName() + "\n" + formatRupiah.format(menu.getPrice()).replace("Rp", "Rp "));
        holder.itemView.setOnClickListener(v -> listener.onMenuClick(menu));
    }

    @Override
    public int getItemCount() { return menuList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        public ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(android.R.id.text1);
            tvTitle.setTextColor(Color.WHITE); // Teks putih agar terlihat di background gelap
        }
    }
}
