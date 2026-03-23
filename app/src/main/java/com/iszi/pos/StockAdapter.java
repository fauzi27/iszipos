package com.iszi.pos;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.ViewHolder> {

    private List<MenuModel> menuList;
    private OnStockClickListener listener;

    public interface OnStockClickListener {
        void onMinClick(MenuModel menu);
        void onPlusClick(MenuModel menu);
        void onNumberClick(MenuModel menu);
    }

    public StockAdapter(List<MenuModel> menuList, OnStockClickListener listener) {
        this.menuList = menuList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuModel menu = menuList.get(position);
        
        holder.tvMenuNameStock.setText(menu.getName());
        holder.tvStockValue.setText(String.valueOf(menu.getStock()));
        holder.tvMenuStockCount.setText("Sisa Stok: " + menu.getStock());

        // Peringatan Stok Menipis (Merah jika < 5, Hijau jika aman)
        if (menu.getStock() < 5) {
            holder.tvMenuStockCount.setTextColor(Color.parseColor("#EF4444"));
        } else {
            holder.tvMenuStockCount.setTextColor(Color.parseColor("#4ADE80"));
        }

        // Dummy Icon (Gallery)
        holder.imgMenuStock.setImageResource(android.R.drawable.ic_menu_gallery);

        // Aksi Tombol
        holder.btnMinStock.setOnClickListener(v -> listener.onMinClick(menu));
        holder.btnPlusStock.setOnClickListener(v -> listener.onPlusClick(menu));
        holder.tvStockValue.setOnClickListener(v -> listener.onNumberClick(menu));
    }

    @Override
    public int getItemCount() { return menuList.size(); }

    public void updateList(List<MenuModel> newList) {
        menuList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMenuNameStock, tvMenuStockCount, tvStockValue;
        ImageButton btnMinStock, btnPlusStock;
        ImageView imgMenuStock;

        public ViewHolder(View itemView) {
            super(itemView);
            tvMenuNameStock = itemView.findViewById(R.id.tvMenuNameStock);
            tvMenuStockCount = itemView.findViewById(R.id.tvMenuStockCount);
            tvStockValue = itemView.findViewById(R.id.tvStockValue);
            btnMinStock = itemView.findViewById(R.id.btnMinStock);
            btnPlusStock = itemView.findViewById(R.id.btnPlusStock);
            imgMenuStock = itemView.findViewById(R.id.imgMenuStock);
        }
    }
}
