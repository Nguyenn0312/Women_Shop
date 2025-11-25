package com.example.women_shop;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnect {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/WomenShop";
    private static final String USER = "root";      // your MySQL username
    private static final String PASS = "root";          // your MySQL password

    public static Connection connect() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}