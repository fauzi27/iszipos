package com.iszi.pos;

public class MenuModel {
    private String id, name, category, image, icon;
    private int price, stock, capitalPrice;

    // Konstruktor kosong wajib untuk Firebase
    public MenuModel() {}

    public MenuModel(String id, String name, String category, int price, int stock, int capitalPrice) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.capitalPrice = capitalPrice;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getPrice() { return price; }
    public int getStock() { return stock; }
    public int getCapitalPrice() { return capitalPrice; }
    public String getImage() { return image; }
    public String getIcon() { return icon; }
}
