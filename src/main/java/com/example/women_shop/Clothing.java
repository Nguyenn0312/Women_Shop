package com.example.women_shop;

public class Clothing extends Product {
    private int size;

    public Clothing(int id, String name, double purchasePrice, double salePrice,
                    double discountPrice, int quantity, String status, int size) {
        super(id, name, purchasePrice, salePrice, discountPrice, quantity, status);
        setSize(size);
    }

    @Override
    public String getCategory() { return "Clothing"; }

    @Override
    public String getSizeString() { return String.valueOf(size); }

    public int getSize() { return size; }

    public void setSize(int size) {
        if (size < 34 || size > 54)
            throw new IllegalArgumentException("Clothing size must be between 34 and 54");
        this.size = size;
    }
}