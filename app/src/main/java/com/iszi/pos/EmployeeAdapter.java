package com.iszi.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {

    // Model Karyawan Internal
    public static class EmployeeModel {
        public String id, name, email;
        public Map<String, Boolean> accessRights;
        public EmployeeModel(String id, String name, String email, Map<String, Boolean> accessRights) {
            this.id = id; this.name = name; this.email = email; this.accessRights = accessRights;
        }
    }

    public interface EmployeeActionListener {
        void onUpdateAccess(String empId, String accessKey, boolean newValue);
        void onDelete(String empId, String empName);
    }

    private List<EmployeeModel> list;
    private EmployeeActionListener listener;

    public EmployeeAdapter(List<EmployeeModel> list, EmployeeActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmployeeModel emp = list.get(position);
        holder.tvEmpName.setText(emp.name);
        holder.tvEmpEmail.setText(emp.email);

        // Set status Checkbox berdasarkan data Firebase
        boolean hasCashier = emp.accessRights != null && Boolean.TRUE.equals(emp.accessRights.get("cashier"));
        boolean hasCalc = emp.accessRights != null && Boolean.TRUE.equals(emp.accessRights.get("calculator"));
        boolean hasReport = emp.accessRights != null && Boolean.TRUE.equals(emp.accessRights.get("report"));
        boolean hasStock = emp.accessRights != null && Boolean.TRUE.equals(emp.accessRights.get("stock"));
        boolean hasTable = emp.accessRights != null && Boolean.TRUE.equals(emp.accessRights.get("table"));
        boolean hasMenu = emp.accessRights != null && Boolean.TRUE.equals(emp.accessRights.get("admin"));
        boolean hasSettings = emp.accessRights != null && Boolean.TRUE.equals(emp.accessRights.get("settings"));

        // Lepas listener sementara agar tidak memicu update saat di-scroll
        holder.chkAccessCashier.setOnCheckedChangeListener(null);
        holder.chkAccessCalculator.setOnCheckedChangeListener(null);
        holder.chkAccessReport.setOnCheckedChangeListener(null);
        holder.chkAccessStock.setOnCheckedChangeListener(null);
        holder.chkAccessTable.setOnCheckedChangeListener(null);
        holder.chkAccessMenu.setOnCheckedChangeListener(null);
        holder.chkAccessSettings.setOnCheckedChangeListener(null);

        holder.chkAccessCashier.setChecked(hasCashier);
        holder.chkAccessCalculator.setChecked(hasCalc);
        holder.chkAccessReport.setChecked(hasReport);
        holder.chkAccessStock.setChecked(hasStock);
        holder.chkAccessTable.setChecked(hasTable);
        holder.chkAccessMenu.setChecked(hasMenu);
        holder.chkAccessSettings.setChecked(hasSettings);

        // Pasang listener aksi
        holder.chkAccessCashier.setOnCheckedChangeListener((btn, isChecked) -> listener.onUpdateAccess(emp.id, "cashier", isChecked));
        holder.chkAccessCalculator.setOnCheckedChangeListener((btn, isChecked) -> listener.onUpdateAccess(emp.id, "calculator", isChecked));
        holder.chkAccessReport.setOnCheckedChangeListener((btn, isChecked) -> listener.onUpdateAccess(emp.id, "report", isChecked));
        holder.chkAccessStock.setOnCheckedChangeListener((btn, isChecked) -> listener.onUpdateAccess(emp.id, "stock", isChecked));
        holder.chkAccessTable.setOnCheckedChangeListener((btn, isChecked) -> listener.onUpdateAccess(emp.id, "table", isChecked));
        holder.chkAccessMenu.setOnCheckedChangeListener((btn, isChecked) -> listener.onUpdateAccess(emp.id, "admin", isChecked));
        holder.chkAccessSettings.setOnCheckedChangeListener((btn, isChecked) -> listener.onUpdateAccess(emp.id, "settings", isChecked));

        holder.btnDeleteEmp.setOnClickListener(v -> listener.onDelete(emp.id, emp.name));
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmpName, tvEmpEmail;
        ImageButton btnDeleteEmp;
        CheckBox chkAccessCashier, chkAccessCalculator, chkAccessReport, chkAccessStock, chkAccessTable, chkAccessMenu, chkAccessSettings;

        public ViewHolder(View v) {
            super(v);
            tvEmpName = v.findViewById(R.id.tvEmpName);
            tvEmpEmail = v.findViewById(R.id.tvEmpEmail);
            btnDeleteEmp = v.findViewById(R.id.btnDeleteEmp);
            chkAccessCashier = v.findViewById(R.id.chkAccessCashier);
            chkAccessCalculator = v.findViewById(R.id.chkAccessCalculator);
            chkAccessReport = v.findViewById(R.id.chkAccessReport);
            chkAccessStock = v.findViewById(R.id.chkAccessStock);
            chkAccessTable = v.findViewById(R.id.chkAccessTable);
            chkAccessMenu = v.findViewById(R.id.chkAccessMenu);
            chkAccessSettings = v.findViewById(R.id.chkAccessSettings);
        }
    }
}
