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

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private List<CartModel> cartList;
    private OnCartChangeListener listener;
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    public interface OnCartChangeListener {
        void onQtyChange(CartModel item, int change);
    }

    public CartAdapter(List<CartModel> cartList, OnCartChangeListener listener) {
        this.cartList = cartList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartModel item = cartList.get(position);
        holder.tvCartItemName.setText(item.getName());
        holder.tvCartQty.setText(String.valueOf(item.getQty()));
        
        String priceFormatted = formatRupiah.format(item.getPrice()).replace("Rp", "Rp ");
        holder.tvCartItemSub.setText(item.getQty() + " x " + priceFormatted);
        
        String totalFormatted = formatRupiah.format((long) item.getPrice() * item.getQty()).replace("Rp", "Rp ");
        holder.tvCartItemTotal.setText(totalFormatted);

        holder.btnQtyMin.setOnClickListener(v -> listener.onQtyChange(item, -1));
        holder.btnQtyPlus.setOnClickListener(v -> listener.onQtyChange(item, 1));
    }

    @Override
    public int getItemCount() { return cartList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCartItemName, tvCartItemSub, tvCartItemTotal, tvCartQty, btnQtyMin, btnQtyPlus;
        public ViewHolder(View itemView) {
            super(itemView);
            tvCartItemName = itemView.findViewById(R.id.tvCartItemName);
            tvCartItemSub = itemView.findViewById(R.id.tvCartItemSub);
            tvCartItemTotal = itemView.findViewById(R.id.tvCartItemTotal);
            tvCartQty = itemView.findViewById(R.id.tvCartQty);
            btnQtyMin = itemView.findViewById(R.id.btnQtyMin);
            btnQtyPlus = itemView.findViewById(R.id.btnQtyPlus);
        }
    }
}
