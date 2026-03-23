package com.iszi.pos;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MenuCashierAdapter extends RecyclerView.Adapter<MenuCashierAdapter.ViewHolder> {

    private List<MenuModel> menuList;
    private OnMenuClickListener listener;
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    public interface OnMenuClickListener { void onMenuClick(MenuModel menu); }

    public MenuCashierAdapter(List<MenuModel> menuList, OnMenuClickListener listener) {
        this.menuList = menuList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_cashier, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuModel menu = menuList.get(position);
        holder.tvMenuName.setText(menu.getName());
        holder.tvMenuPrice.setText(formatRupiah.format(menu.getPrice()).replace("Rp", "Rp "));
        
        if (menu.getStock() > 0) {
            holder.tvMenuStock.setText("Stok: " + menu.getStock());
            holder.tvMenuStock.setTextColor(Color.parseColor("#9CA3AF"));
        } else {
            holder.tvMenuStock.setText("Habis!");
            holder.tvMenuStock.setTextColor(Color.parseColor("#EF4444"));
        }

        holder.imgMenu.setImageResource(android.R.drawable.ic_menu_gallery);
        
        holder.itemView.setOnClickListener(v -> {
            // Cerdas: Hanya bisa diklik masuk keranjang jika stok masih ada
            if (menu.getStock() > 0) {
                listener.onMenuClick(menu);
            }
        });
    }

    @Override
    public int getItemCount() { return menuList.size(); }

    public void updateList(List<MenuModel> newList) {
        menuList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMenuName, tvMenuPrice, tvMenuStock;
        ImageView imgMenu;
        public ViewHolder(View itemView) {
            super(itemView);
            tvMenuName = itemView.findViewById(R.id.tvMenuName);
            tvMenuPrice = itemView.findViewById(R.id.tvMenuPrice);
            tvMenuStock = itemView.findViewById(R.id.tvMenuStock);
            imgMenu = itemView.findViewById(R.id.imgMenu);
        }
    }
}
