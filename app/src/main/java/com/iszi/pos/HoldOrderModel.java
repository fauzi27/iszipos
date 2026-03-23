package com.iszi.pos;

import java.util.List;

public class HoldOrderModel {
    private String buyerName;
    private long timestamp;
    private List<MenuModel> items;

    public HoldOrderModel(String buyerName, long timestamp, List<MenuModel> items) {
        this.buyerName = buyerName;
        this.timestamp = timestamp;
        this.items = items;
    }

    public String getBuyerName() { return buyerName; }
    public long getTimestamp() { return timestamp; }
    public List<MenuModel> getItems() { return items; }
}
