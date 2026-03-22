package com.iszi.pos;

public class CartModel {
    private String id, name;
    private int price, qty, maxStock;

    public CartModel(String id, String name, int price, int qty, int maxStock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.qty = qty;
        this.maxStock = maxStock;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getQty() { return qty; }
    public int getMaxStock() { return maxStock; }

    public void setQty(int qty) { this.qty = qty; }
}
