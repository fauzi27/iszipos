package com.iszi.pos;

public class ClientModel {
    private String id, name, shopName, email, plan;
    private boolean isSuspended;
    private long expiredAt;
    private int maxTransactions, maxMenus, maxImages;
    private int currentUsage, menuUsage, imageUsage, staffCount;

    public ClientModel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    
    public boolean isSuspended() { return isSuspended; }
    public void setSuspended(boolean suspended) { isSuspended = suspended; }
    
    public long getExpiredAt() { return expiredAt; }
    public void setExpiredAt(long expiredAt) { this.expiredAt = expiredAt; }

    public int getMaxTransactions() { return maxTransactions; }
    public void setMaxTransactions(int max) { this.maxTransactions = max; }

    public int getMaxMenus() { return maxMenus; }
    public void setMaxMenus(int max) { this.maxMenus = max; }

    public int getMaxImages() { return maxImages; }
    public void setMaxImages(int max) { this.maxImages = max; }

    // Data tambahan hasil kalkulasi
    public int getCurrentUsage() { return currentUsage; }
    public void setCurrentUsage(int usage) { this.currentUsage = usage; }

    public int getMenuUsage() { return menuUsage; }
    public void setMenuUsage(int usage) { this.menuUsage = usage; }

    public int getImageUsage() { return imageUsage; }
    public void setImageUsage(int usage) { this.imageUsage = usage; }

    public int getStaffCount() { return staffCount; }
    public void setStaffCount(int count) { this.staffCount = count; }
}
