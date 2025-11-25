package com.example.women_shop;

public abstract class Product {

    protected int id;
    protected String name;
    protected double purchasePrice;
    protected double salePrice;
    protected double discountPrice;
    protected int quantity;
    protected String status;

    public Product(int id, String name, double purchasePrice, double salePrice,
                   double discountPrice, int quantity, String status) {
        this.id = id;
        this.name = name;
        this.purchasePrice = purchasePrice;
        this.salePrice = salePrice;
        this.discountPrice = discountPrice;
        this.quantity = quantity;
        this.status = status;
    }

    public double getEffectiveSalePrice() {
        return discountPrice > 0 ? discountPrice : salePrice;
    }

    public abstract String getCategory();

    public String getSizeString() { return ""; }

    public double getPrice() { return salePrice; }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getPurchasePrice() { return purchasePrice; }
    public double getSalePrice() { return salePrice; }
    public double getDiscountPrice() { return discountPrice; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }

    public void setName(String name) {
        this.name = name;
    }

    public void setPurchasePrice(double purchasePrice) {
        if (purchasePrice < 0)
            throw new IllegalArgumentException("Purchase price cannot be negative");

        if (this.salePrice > 0 && purchasePrice > this.salePrice)
            throw new IllegalArgumentException("Purchase price cannot be greater than sale price");

        this.purchasePrice = purchasePrice;
    }

    public void setSalePrice(double salePrice) {
        if (salePrice < 0)
            throw new IllegalArgumentException("Sale price cannot be negative");

        if (purchasePrice > salePrice)
            throw new IllegalArgumentException("Sale price cannot be lower than purchase price");

        this.salePrice = salePrice;
    }

    public void setDiscountPrice(double discountPrice) {
        if (discountPrice < 0)
            throw new IllegalArgumentException("Discount price cannot be negative");

        this.discountPrice = discountPrice;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    protected void increaseStock(int q) {
        if (q < 0) throw new IllegalArgumentException("Quantity cannot be negative");
        quantity += q;
    }

    protected void decreaseStock(int q) {
        if (q < 0) throw new IllegalArgumentException("Quantity cannot be negative");
        if (q > quantity) throw new IllegalArgumentException("Not enough stock");
        quantity -= q;
    }
}