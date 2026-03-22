package com.iszi.pos;

public class MenuModel {
    private String id;
    private String name;
    private String category;
    private int price;
    private int capitalPrice;
    private int stock;
    private String image;

    // Konstruktor kosong (Wajib ada untuk Firebase Firestore)
    public MenuModel() {
    }

    public MenuModel(String id, String name, String category, int price, int capitalPrice, int stock, String image) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.capitalPrice = capitalPrice;
        this.stock = stock;
        this.image = image;
    }

    // Getter untuk mengambil data
    public String getId() { return id; }
    public void setId(String id) { this.id = id; } // Setter ID perlu untuk Firestore ID
    
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getPrice() { return price; }
    public int getCapitalPrice() { return capitalPrice; }
    public int getStock() { return stock; }
    public String getImage() { return image; }
}
