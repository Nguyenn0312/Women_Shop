package com.example.women_shop;

public class Accessory extends Product {

    public Accessory(int id, String name, double purchasePrice, double salePrice,
                     double discountPrice, int quantity, String status) {
        super(id, name, purchasePrice, salePrice, discountPrice, quantity, status);
    }

    @Override
    public String getCategory() { return "Accessory"; }
}