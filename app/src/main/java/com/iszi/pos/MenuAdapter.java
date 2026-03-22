package com.iszi.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {

    private List<MenuModel> menuList;
    private OnItemClickListener listener;

    // Interface untuk menangkap aksi klik (Edit / Hapus) ke AdminActivity nanti
    public interface OnItemClickListener {
        void onEditClick(MenuModel menu);
        void onDeleteClick(MenuModel menu);
    }

    // Konstruktor
    public MenuAdapter(List<MenuModel> menuList, OnItemClickListener listener) {
        this.menuList = menuList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Menyambungkan dengan desain item_menu.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        MenuModel menu = menuList.get(position);

        // Pasang teks nama
        holder.tvMenuName.setText(menu.getName());
        
        // Pasang kategori (ubah jadi huruf besar semua agar rapi)
        String category = menu.getCategory() != null ? menu.getCategory().toUpperCase() : "UMUM";
        holder.tvMenuCategory.setText(category);

        // Format angka menjadi Rupiah (otomatis ada titik pemisahnya)
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        holder.tvMenuPrice.setText(formatRupiah.format(menu.getPrice()).replace("Rp", "Rp "));

        /* * Catatan Gambar:
         * Sementara kita pakaikan ikon bawaan. Nanti untuk me-load URL gambar (Cloudinary/Firebase), 
         * kita akan menggunakan library tambahan bernama 'Glide' agar tidak berat.
         */
        holder.imgMenu.setImageResource(android.R.drawable.ic_menu_gallery);

        // Jika tombol Edit diklik
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(menu);
        });

        // Jika tombol Hapus diklik
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(menu);
        });
    }

    @Override
    public int getItemCount() {
        return menuList.size();
    }

    // Fungsi penting untuk fitur Pencarian (Search) / Filter Kategori
    public void updateList(List<MenuModel> newList) {
        menuList = newList;
        notifyDataSetChanged(); // Memberi tahu RecyclerView bahwa data berubah
    }

    // Kelas untuk memegang komponen UI dari XML
    public static class MenuViewHolder extends RecyclerView.ViewHolder {
        TextView tvMenuName, tvMenuCategory, tvMenuPrice;
        ImageView imgMenu;
        ImageButton btnEdit, btnDelete;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMenuName = itemView.findViewById(R.id.tvMenuName);
            tvMenuCategory = itemView.findViewById(R.id.tvMenuCategory);
            tvMenuPrice = itemView.findViewById(R.id.tvMenuPrice);
            imgMenu = itemView.findViewById(R.id.imgMenu);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
