package com.example.women_shop;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private final double INITIAL_CAPITAL = 30000.00;

    private static final String SELECT_ALL_PRODUCTS_SQL =
            "SELECT p.id, p.name, p.purchase_price, p.sale_price, p.discount_price, p.stock, p.status, " +
                    "c.size AS clothing_size, s.size AS shoe_size, " +
                    "CASE " +
                    "    WHEN c.product_id IS NOT NULL THEN 'Clothing' " +
                    "    WHEN s.product_id IS NOT NULL THEN 'Shoes' " +
                    "    WHEN a.product_id IS NOT NULL THEN 'Accessory' " +
                    "    ELSE 'Unknown' " +
                    "END AS category " +
                    "FROM product p " +
                    "LEFT JOIN clothing c ON p.id = c.product_id " +
                    "LEFT JOIN shoes s ON p.id = s.product_id " +
                    "LEFT JOIN accessory a ON p.id = a.product_id " +
                    "ORDER BY p.id";

    public List<Product> loadAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();

        try (Connection conn = DBConnect.connect();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_PRODUCTS_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double purchasePrice = rs.getDouble("purchase_price");
                double salePrice = rs.getDouble("sale_price");
                double discountPrice = rs.getDouble("discount_price");
                int stock = rs.getInt("stock");
                String status = rs.getString("status");
                String category = rs.getString("category");

                Product product = switch (category) {
                    case "Clothing" -> new Clothing(id, name, purchasePrice, salePrice, discountPrice, stock, status, rs.getInt("clothing_size"));
                    case "Shoes" -> new Shoes(id, name, purchasePrice, salePrice, discountPrice, stock, status, rs.getInt("shoe_size"));
                    case "Accessory" -> new Accessory(id, name, purchasePrice, salePrice, discountPrice, stock, status);
                    default -> throw new IllegalStateException("Catégorie de produit inattendue: " + category);
                };
                products.add(product);
            }
        }
        return products;
    }

    public void addProduct(Product p, String category, String size) throws SQLException {
        String productSql = "INSERT INTO product (name, purchase_price, sale_price, stock, status) VALUES (?, ?, ?, 0, 'Soldout')";
        String categorySql = null;

        switch (category) {
            case "Clothing" -> categorySql = "INSERT INTO clothing (product_id, size) VALUES (?, ?)";
            case "Shoes" -> categorySql = "INSERT INTO shoes (product_id, size) VALUES (?, ?)";
            case "Accessory" -> categorySql = "INSERT INTO accessory (product_id) VALUES (?)";
            default -> throw new IllegalArgumentException("Catégorie de produit invalide.");
        }

        Connection conn = null;
        try {
            conn = DBConnect.connect();
            conn.setAutoCommit(false);

            try (PreparedStatement psProduct = conn.prepareStatement(productSql, Statement.RETURN_GENERATED_KEYS)) {
                psProduct.setString(1, p.getName());
                psProduct.setDouble(2, p.getPurchasePrice());
                psProduct.setDouble(3, p.getSalePrice());

                int affectedRows = psProduct.executeUpdate();
                if (affectedRows == 0) throw new SQLException("Échec de la création du produit, aucune ligne affectée.");

                try (ResultSet generatedKeys = psProduct.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int productId = generatedKeys.getInt(1);
                        p.id = productId;

                        try (PreparedStatement psCategory = conn.prepareStatement(categorySql)) {
                            psCategory.setInt(1, productId);
                            if (category.equals("Clothing") || category.equals("Shoes")) {
                                psCategory.setInt(2, Integer.parseInt(size));
                            }
                            psCategory.executeUpdate();
                            conn.commit();
                        }
                    } else throw new SQLException("Échec de la création du produit, aucun ID obtenu.");
                }
            }
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public void updateProduct(Product p, String category, String size) throws SQLException {
        String productSql = "UPDATE product SET name=?, purchase_price=? WHERE id=?";
        String categorySql = null;

        switch (category) {
            case "Clothing" -> categorySql = "UPDATE clothing SET size=? WHERE product_id=?";
            case "Shoes" -> categorySql = "UPDATE shoes SET size=? WHERE product_id=?";
        }

        Connection conn = null;

        try {
            conn = DBConnect.connect();
            conn.setAutoCommit(false);

            try (PreparedStatement psProduct = conn.prepareStatement(productSql)) {
                psProduct.setString(1, p.getName());
                psProduct.setDouble(2, p.getPurchasePrice());
                psProduct.setInt(3, p.getId());
                psProduct.executeUpdate();
            }

            if (categorySql != null) {
                try (PreparedStatement psCategory = conn.prepareStatement(categorySql)) {
                    psCategory.setInt(1, Integer.parseInt(size));
                    psCategory.setInt(2, p.getId());
                    psCategory.executeUpdate();
                }
            }
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public void deleteProduct(int productId) throws SQLException {
        if (getProductStock(productId) > 0) {
            throw new IllegalStateException("Product cannot be deleted: stock still available");
        }
        String sql = "DELETE FROM product WHERE id = ?";
        try (Connection conn = DBConnect.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }

    public void recordTransaction(int productId, String type, int quantity, double price_unitaire) throws SQLException {
        String sql = "INSERT INTO transactions (product_id, type, quantity, price) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnect.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setString(2, type);
            ps.setInt(3, quantity);
            ps.setDouble(4, price_unitaire);
            ps.executeUpdate();
        }
    }

    public double getInitialCapital() { return INITIAL_CAPITAL; }

    public double calculateIncome() throws SQLException {
        double income = 0;
        String sql = "SELECT SUM(total_price) FROM transactions WHERE type='SELL'";
        try (Connection conn = DBConnect.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) income = rs.getDouble(1);
        }
        return income;
    }

    public double calculateCost() throws SQLException {
        double cost = 0;
        String sql = "SELECT SUM(total_price) FROM transactions WHERE type='BUY'";
        try (Connection conn = DBConnect.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) cost = rs.getDouble(1);
        }
        return cost;
    }

    public double calculateCapital(double income, double cost) {
        return INITIAL_CAPITAL + income - cost;
    }

    public void setDiscountPriceByCategory(String category, double discountRate) throws SQLException {
        String baseSql = "UPDATE product SET discount_price = sale_price * ? WHERE id IN (";
        String productIdsSql;

        switch (category) {
            case "Clothing" -> productIdsSql = "SELECT product_id FROM clothing";
            case "Shoes" -> productIdsSql = "SELECT product_id FROM shoes";
            case "Accessory" -> productIdsSql = "SELECT product_id FROM accessory";
            default -> throw new IllegalArgumentException("Catégorie invalide pour l'application des réductions.");
        }

        String sql = baseSql + productIdsSql + ")";

        try (Connection conn = DBConnect.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, discountRate);
            ps.executeUpdate();
        }
    }

    public void clearAllDiscountPrices() throws SQLException {
        String sql = "UPDATE product SET discount_price = 0 WHERE discount_price > 0";
        try (Connection conn = DBConnect.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private int getProductStock(int productId) throws SQLException {
        String sql = "SELECT stock FROM product WHERE id = ?";
        try (Connection conn = DBConnect.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("stock") : 0;
            }
        }
    }
}