package com.iszi.pos;

public class TransactionModel {
    private String id, buyer, date, method, status, operatorName;
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
    public String getDate() { return date; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    public int getTotal() { return total; }
    public int getPaid() { return paid; }
    public int getRemaining() { return remaining; }
    public int getCapitalTotal() { return capitalTotal; }
    public boolean isPendingSync() { return isPendingSync; }
}
