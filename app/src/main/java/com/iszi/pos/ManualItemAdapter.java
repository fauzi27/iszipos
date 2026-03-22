package com.iszi.pos;

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

public class ManualItemAdapter extends RecyclerView.Adapter<ManualItemAdapter.ViewHolder> {

    private List<ManualItemModel> itemList;
    private OnItemRemoveListener listener;
    private NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    public interface OnItemRemoveListener { void onRemove(ManualItemModel item); }

    public ManualItemAdapter(List<ManualItemModel> itemList, OnItemRemoveListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manual, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ManualItemModel item = itemList.get(position);
        holder.tvItemName.setText(item.getName());
        
        String priceFormatted = formatRupiah.format(item.getPrice()).replace("Rp", "Rp ");
        holder.tvItemSubPrice.setText(item.getQty() + " x " + priceFormatted);
        
        String totalFormatted = formatRupiah.format((long) item.getPrice() * item.getQty()).replace("Rp", "Rp ");
        holder.tvItemTotalPrice.setText(totalFormatted);

        holder.btnRemove.setOnClickListener(v -> listener.onRemove(item));
    }

    @Override
    public int getItemCount() { return itemList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemSubPrice, tvItemTotalPrice;
        ImageButton btnRemove;
        public ViewHolder(View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemSubPrice = itemView.findViewById(R.id.tvItemSubPrice);
            tvItemTotalPrice = itemView.findViewById(R.id.tvItemTotalPrice);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
