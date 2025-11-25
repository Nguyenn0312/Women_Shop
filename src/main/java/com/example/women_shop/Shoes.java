package com.example.women_shop;

public class Shoes extends Product {
    private int size; // 36–50

    public Shoes(int id, String name, double purchasePrice, double salePrice,
                 double discountPrice, int quantity, String status, int size) {
        super(id, name, purchasePrice, salePrice, discountPrice, quantity, status);
        setSize(size);
    }

    @Override
    public String getCategory() { return "Shoes"; }

    @Override
    public String getSizeString() { return String.valueOf(size); }

    public int getSize() { return size; }

    public void setSize(int size) {
        if (size < 36 || size > 50)
            throw new IllegalArgumentException("Shoe size must be between 36 and 50");
        this.size = size;
    }
}