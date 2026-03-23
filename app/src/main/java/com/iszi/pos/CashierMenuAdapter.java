package com.iszi.pos;

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
        // 🔥 INI PERUBAHANNYA: Memanggil desain Kartu (Card) yang baru 🔥
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_cashier, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuModel menu = menuList.get(position);
        
        holder.tvMenuName.setText(menu.getName());
        holder.tvMenuPrice.setText(formatRupiah.format(menu.getPrice()).replace("Rp", "Rp "));
        
        holder.itemView.setOnClickListener(v -> listener.onMenuClick(menu));
    }

    @Override
    public int getItemCount() { return menuList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMenuName, tvMenuPrice;
        
        public ViewHolder(View itemView) {
            super(itemView);
            // Menyesuaikan ID dengan di item_menu_cashier.xml
            tvMenuName = itemView.findViewById(R.id.tvMenuName);
            tvMenuPrice = itemView.findViewById(R.id.tvMenuPrice);
        }
    }
}
