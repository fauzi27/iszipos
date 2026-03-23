package com.iszi.pos;

public class TransactionModel {
    private String id, buyer, date, method, status, operatorName, refundReason;
    private long timestamp;
    private int total, paid, remaining, capitalTotal;
    private boolean isPendingSync;

    public TransactionModel() {} // Wajib untuk Firebase

    public TransactionModel(String id, String buyer, String date, String method, String status, long timestamp, int total, int paid, int remaining, int capitalTotal, boolean isPendingSync) {
        this.id = id;
        this.buyer = buyer;
        this.date = date;
        this.method = method;
        this.status = status;
        this.timestamp = timestamp;
        this.total = total;
        this.paid = paid;
        this.remaining = remaining;
        this.capitalTotal = capitalTotal; // Total HPP / Modal
        this.isPendingSync = isPendingSync;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBuyer() { return buyer; }
    public void setBuyer(String buyer) { this.buyer = buyer; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getPaid() { return paid; }
    public void setPaid(int paid) { this.paid = paid; }
    public int getRemaining() { return remaining; }
    public void setRemaining(int remaining) { this.remaining = remaining; }
    public int getCapitalTotal() { return capitalTotal; }
    public void setCapitalTotal(int capitalTotal) { this.capitalTotal = capitalTotal; }
    public boolean isPendingSync() { return isPendingSync; }
    public void setPendingSync(boolean pendingSync) { this.isPendingSync = pendingSync; }

    // 🔥 INI FUNGSI YANG KETINGGALAN SEBELUMNYA 🔥
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
}
