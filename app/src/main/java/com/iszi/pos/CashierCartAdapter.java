package com.iszi.pos;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CashierCartAdapter extends RecyclerView.Adapter<CashierCartAdapter.ViewHolder> {

    private List<MenuModel> cartList;
    private Runnable onCartUpdated;
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    public CashierCartAdapter(List<MenuModel> cartList, Runnable onCartUpdated) {
        this.cartList = cartList;
        this.onCartUpdated = onCartUpdated;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Karena kita tidak punya file item_cart.xml, kita buat UI-nya langsung dari Java agar cepat dan rapi
        android.widget.LinearLayout layout = new android.widget.LinearLayout(parent.getContext());
        layout.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setPadding(16, 16, 16, 16);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(parent.getContext());
        tvName.setId(android.R.id.text1);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(14f);
        android.widget.LinearLayout.LayoutParams paramsName = new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(paramsName);

        TextView tvQtyPrice = new TextView(parent.getContext());
        tvQtyPrice.setId(android.R.id.text2);
        tvQtyPrice.setTextColor(Color.parseColor("#9CA3AF"));
        tvQtyPrice.setTextSize(12f);
        tvQtyPrice.setPadding(16, 0, 16, 0);

        ImageButton btnDelete = new ImageButton(parent.getContext());
        btnDelete.setId(android.R.id.button1);
        btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
        btnDelete.setBackgroundColor(Color.TRANSPARENT);
        btnDelete.setColorFilter(Color.parseColor("#EF4444"));

        layout.addView(tvName);
        layout.addView(tvQtyPrice);
        layout.addView(btnDelete);

        return new ViewHolder(layout, tvName, tvQtyPrice, btnDelete);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuModel item = cartList.get(position);
        
        holder.tvName.setText(item.getName());
        int subtotal = item.getPrice() * item.getStock(); // getStock() dipakai sebagai QTY sementara
        holder.tvQtyPrice.setText(item.getStock() + "x Rp " + formatRupiah.format(item.getPrice()).replace("Rp", ""));

        holder.btnDelete.setOnClickListener(v -> {
            cartList.remove(position);
            notifyDataSetChanged();
            onCartUpdated.run();
        });
    }

    @Override
    public int getItemCount() { return cartList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvQtyPrice;
        ImageButton btnDelete;

        public ViewHolder(View itemView, TextView tvName, TextView tvQtyPrice, ImageButton btnDelete) {
            super(itemView);
            this.tvName = tvName;
            this.tvQtyPrice = tvQtyPrice;
            this.btnDelete = btnDelete;
        }
    }
}
