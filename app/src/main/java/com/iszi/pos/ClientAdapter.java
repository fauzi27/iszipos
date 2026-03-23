package com.iszi.pos;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class ClientAdapter extends RecyclerView.Adapter<ClientAdapter.ViewHolder> {

    private List<ClientModel> clientList;
    private OnGodModeListener listener;

    public interface OnGodModeListener {
        void onTogglePlan(ClientModel client);
        void onInjectData(ClientModel client);
        void onEditLimit(ClientModel client);
        void onToggleSuspend(ClientModel client);
        void onDeleteClient(ClientModel client);
    }

    public ClientAdapter(List<ClientModel> clientList, OnGodModeListener listener) {
        this.clientList = clientList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_superadmin_client, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClientModel client = clientList.get(position);

        String displayName = client.getShopName() != null ? client.getShopName() : client.getName();
        holder.tvClientName.setText(displayName != null ? displayName : "Toko Tanpa Nama");
        holder.tvClientEmail.setText(client.getEmail() != null ? client.getEmail() : "No Email");

        // Set Plan Badge (PRO/FREE)
        boolean isPremium = "premium".equals(client.getPlan());
        if (isPremium) {
            holder.badgePlan.setText("👑 PRO");
            holder.badgePlan.setTextColor(Color.parseColor("#FEF08A")); // Kuning
            holder.badgePlan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#854D0E")));
            holder.btnGodPlan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#B45309")));
        } else {
            holder.badgePlan.setText("🆓 FREE");
            holder.badgePlan.setTextColor(Color.parseColor("#E2E8F0")); // Abu-abu
            holder.badgePlan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#334155")));
            holder.btnGodPlan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#374151")));
        }

        // Set Status Badge
        boolean isExpired = client.getExpiredAt() > 0 && client.getExpiredAt() < System.currentTimeMillis();
        if (isExpired) {
            holder.badgeStatus.setText("🔴 EXPIRED");
            holder.badgeStatus.setTextColor(Color.parseColor("#FCA5A5"));
            holder.badgeStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#7F1D1D")));
        } else if (client.isSuspended()) {
            holder.badgeStatus.setText("⏸️ SUSPENDED");
            holder.badgeStatus.setTextColor(Color.parseColor("#FDBA74"));
            holder.badgeStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#7C2D12")));
            // Ubah tombol suspend jadi hijau untuk "Unsuspend"
            holder.btnGodSuspend.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#064E3B")));
            holder.btnGodSuspend.setText("Aktifkan");
        } else {
            holder.badgeStatus.setText("🟢 ACTIVE");
            holder.badgeStatus.setTextColor(Color.parseColor("#A7F3D0"));
            holder.badgeStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#065F46")));
            holder.btnGodSuspend.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#7C2D12")));
            holder.btnGodSuspend.setText("Suspend");
        }

        // Action Buttons
        holder.btnGodPlan.setOnClickListener(v -> listener.onTogglePlan(client));
        holder.btnGodInject.setOnClickListener(v -> listener.onInjectData(client));
        holder.btnGodEdit.setOnClickListener(v -> listener.onEditLimit(client));
        holder.btnGodSuspend.setOnClickListener(v -> listener.onToggleSuspend(client));
        holder.btnGodDelete.setOnClickListener(v -> listener.onDeleteClient(client));
    }

    @Override
    public int getItemCount() { return clientList.size(); }

    public void updateList(List<ClientModel> newList) {
        clientList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvClientName, tvClientEmail, badgePlan, badgeStatus, tvUsageTx, tvUsageMenu;
        MaterialButton btnGodPlan, btnGodInject, btnGodEdit, btnGodSuspend;
        ImageButton btnGodDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvClientEmail = itemView.findViewById(R.id.tvClientEmail);
            badgePlan = itemView.findViewById(R.id.badgePlan);
            badgeStatus = itemView.findViewById(R.id.badgeStatus);
            tvUsageTx = itemView.findViewById(R.id.tvUsageTx);
            tvUsageMenu = itemView.findViewById(R.id.tvUsageMenu);

            btnGodPlan = itemView.findViewById(R.id.btnGodPlan);
            btnGodInject = itemView.findViewById(R.id.btnGodInject);
            btnGodEdit = itemView.findViewById(R.id.btnGodEdit);
            btnGodSuspend = itemView.findViewById(R.id.btnGodSuspend);
            btnGodDelete = itemView.findViewById(R.id.btnGodDelete);
        }
    }
}
