package com.howlstudio.servershop;

public class ShopItem {
    private String id;
    private String displayName;
    private String description;
    private double price;
    private int stock; // -1 = unlimited
    private String command; // command to run on purchase

    public ShopItem(String id, String displayName, String description, double price, int stock, String command) {
        this.id = id; this.displayName = displayName; this.description = description;
        this.price = price; this.stock = stock; this.command = command;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public boolean isAvailable() { return stock == -1 || stock > 0; }
    public boolean purchase() {
        if (stock == -1) return true;
        if (stock <= 0) return false;
        stock--; return true;
    }
    public String getCommand() { return command; }
    public String getPriceStr() { return String.format("%.0f", price) + " coins"; }
    public String toConfig() {
        return id + "|" + displayName + "|" + description + "|" + price + "|" + stock + "|" + command;
    }
    public static ShopItem fromConfig(String line) {
        String[] p = line.split("\\|", 6);
        if (p.length < 6) return null;
        return new ShopItem(p[0], p[1], p[2], Double.parseDouble(p[3]), Integer.parseInt(p[4]), p[5]);
    }
}
