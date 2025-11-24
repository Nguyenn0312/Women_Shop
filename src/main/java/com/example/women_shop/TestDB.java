package com.example.women_shop;
import java.sql.Connection;

public class TestDB {
    public static void main(String[] args) {
        Connection c = DBConnect.connect();
        System.out.println("Connection: " + c);
    }
}

