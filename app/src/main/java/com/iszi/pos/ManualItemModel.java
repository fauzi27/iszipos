package com.iszi.pos;

public class ManualItemModel {
    private String id, name;
    private int price, qty;

    public ManualItemModel(String id, String name, int price, int qty) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.qty = qty;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getQty() { return qty; }
}
