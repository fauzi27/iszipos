package com.iszi.pos;

public class MenuModel {
    private String id, name, category, image, icon;
    private int price, stock, capitalPrice;

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
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; } // Ini yang bikin Error!
    public int getCapitalPrice() { return capitalPrice; }
    public void setCapitalPrice(int capitalPrice) { this.capitalPrice = capitalPrice; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
