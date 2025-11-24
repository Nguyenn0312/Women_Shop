package com.example.women_shop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.application.Platform;
import javafx.concurrent.Task;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ShopController {


    @FXML private ComboBox<String> box_Cat;

    @FXML private Button but_Add;
    @FXML private Button but_Reset;
    @FXML private Button but_Modify;
    @FXML private Button but_Delete;
    @FXML private Button but_Purchase;
    @FXML private Button but_Confirm;

    @FXML private TabPane tabPane;
    @FXML private AnchorPane checkoutPane;
    @FXML private ComboBox<String> box_Discount;
    @FXML private Spinner<Integer> spin_PurchaseQuan;
    @FXML private Label label_Subtotal;
    @FXML private Label label_Total;


    @FXML private TextField input_Color;
    @FXML private TextField input_Cond;
    @FXML private TextField input_Name;
    @FXML private TextField input_Price;
    @FXML private TextField input_Quan;
    @FXML private TextField input_Size;
    @FXML private TextField input_Type;

    @FXML private TableView<Clothes> table_List;

    @FXML private TableColumn<Clothes, String> colId;
    @FXML private TableColumn<Clothes, String> colName;
    @FXML private TableColumn<Clothes, String> colCategory;
    @FXML private TableColumn<Clothes, String> colType;
    @FXML private TableColumn<Clothes, String> colColor;
    @FXML private TableColumn<Clothes, String> colSize;
    @FXML private TableColumn<Clothes, String> colCond;
    @FXML private TableColumn<Clothes, Integer> colQuan;
    @FXML private TableColumn<Clothes, Double> colPrice;
    @FXML private TableColumn<Clothes, Double> colStatus;

    private final ObservableList<Clothes> items = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        System.out.println("ShopController.initialize() called: " + this);

        // ComboBox values
        box_Cat.setItems(FXCollections.observableArrayList("Clothes", "Shoes", "Accessory"));
        box_Discount.setItems(FXCollections.observableArrayList("0%","30%", "20%", "50%"));

        // spinner defaults and bounds (min 1)
        SpinnerValueFactory.IntegerSpinnerValueFactory svf =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1);
        spin_PurchaseQuan.setValueFactory(svf);



        // Table column bindings (PropertyValueFactory uses getters in Clothes)
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colColor.setCellValueFactory(new PropertyValueFactory<>("color"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colCond.setCellValueFactory(new PropertyValueFactory<>("condition"));
        colQuan.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        table_List.setItems(items);

        // Load DB data
        loadData();

        // Button handlers
        but_Add.setOnAction(e -> handleAdd());
        but_Reset.setOnAction(e -> handleReset());

        // handlers for new buttons
        but_Modify.setOnAction(e -> handleModify());
        but_Delete.setOnAction(e -> handleDelete());
        but_Purchase.setOnAction(e -> handlePurchase());
        but_Confirm.setOnAction(e -> handleConfirm());


    }

    private void loadData() {
        System.out.println("loadData() start");
        String sql = "SELECT * FROM clothes";

        Connection conn = DBConnect.connect();
        if (conn == null) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not connect to DB. See console.");
            return;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            items.clear();
            while (rs.next()) {
                Clothes c = new Clothes(
                        rs.getInt("idClothes"),
                        rs.getString("product_Name"),
                        rs.getString("product_Category"),
                        rs.getString("product_Type"),
                        rs.getString("product_Color"),
                        rs.getString("product_Size"),
                        rs.getInt("product_Quantity"),
                        rs.getString("product_Condition"),
                        rs.getDouble("product_Price"),
                        rs.getString("product_Status")
                );
                items.add(c);
            }
            System.out.println("Loaded items: " + items.size());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load data. See console.");
        } finally {
            try { conn.close(); } catch (Exception ignored) {}
        }
    }

    private void handleModify() {
        Clothes selected = table_List.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Modify", "Select a product first.");
            return;
        }
        // gather new values from input fields
        String category = box_Cat.getValue();
        String name = input_Name.getText().trim();
        String type = input_Type.getText().trim();
        String color = input_Color.getText().trim();
        String size = input_Size.getText().trim();
        String condition = input_Cond.getText().trim();
        int quantity;
        double price;
        try {
            quantity = Integer.parseInt(input_Quan.getText().trim());
            price = Double.parseDouble(input_Price.getText().trim());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.WARNING, "Modify", "Quantity/Price invalid.");
            return;
        }

        String sql = "UPDATE clothes SET product_Name=?, product_Category=?, product_Type=?, product_Color=?, product_Size=?, product_Quantity=?, product_Condition=?, product_Price=? WHERE idClothes=?";
        Connection conn = DBConnect.connect();
        if (conn == null) { showAlert(Alert.AlertType.ERROR, "DB", "No connection"); return; }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setString(3, type);
            ps.setString(4, color);
            ps.setString(5, size);
            ps.setInt(6, quantity);
            ps.setString(7, condition);
            ps.setDouble(8, price);
            ps.setInt(9, selected.getId());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                loadData(); // reload authoritative data
                showAlert(Alert.AlertType.INFORMATION, "Modify", "Product updated.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Modify", "No rows updated.");
            }
        } catch (Exception ex) { ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "Modify", "Error. See console."); }
        finally { try { conn.close(); } catch (Exception ignored) {} }
    }

    private void handleDelete() {
        Clothes selected = table_List.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert(Alert.AlertType.WARNING, "Delete", "Select a product first."); return; }
        // Optional: confirm deletion via dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete selected product?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        String sql = "DELETE FROM clothes WHERE idClothes=?";
        Connection conn = DBConnect.connect();
        if (conn == null) { showAlert(Alert.AlertType.ERROR, "DB", "No connection"); return; }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selected.getId());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                loadData();
                showAlert(Alert.AlertType.INFORMATION, "Delete", "Product deleted.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Delete", "No rows deleted.");
            }
        } catch (Exception ex) { ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "Delete", "Error. See console."); }
        finally { try { conn.close(); } catch (Exception ignored) {} }
    }
    private void handlePurchase() {
        Clothes selected = table_List.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert(Alert.AlertType.WARNING, "Purchase", "Select a product first."); return; }

        // set default purchase quantity to 1 and max to selected quantity
        int available = selected.getQuantity();
        SpinnerValueFactory.IntegerSpinnerValueFactory svf =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Math.max(1, available), 1);
        spin_PurchaseQuan.setValueFactory(svf);

        // set default discount
        box_Discount.setValue("0%"); // or choose none; you populated earlier with 30/20/50, you can add "0%"
        // show selected price in subtotal
        label_Subtotal.setText(String.format("Subtotal: %.2f", selected.getPrice()));
        label_Total.setText(String.format("Total: %.2f", selected.getPrice()));

        // switch to checkout tab (index 1) — assumes second tab is checkout
        tabPane.getSelectionModel().select(1);
    }

    private void handleConfirm() {
        Clothes selected = table_List.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert(Alert.AlertType.WARNING, "Confirm", "Select a product first."); return; }

        int buyQty = spin_PurchaseQuan.getValue();
        if (buyQty <= 0) { showAlert(Alert.AlertType.WARNING, "Confirm", "Invalid quantity."); return; }

        int currentQty = selected.getQuantity();
        if (buyQty > currentQty) { showAlert(Alert.AlertType.WARNING, "Confirm", "Not enough stock."); return; }

        // compute discount
        String discStr = box_Discount.getValue();
        double discount = 0.0;
        if (discStr != null && discStr.endsWith("%")) {
            try { discount = Double.parseDouble(discStr.replace("%","")) / 100.0; } catch (Exception ignored) {}
        }

        double subtotal = selected.getPrice() * buyQty;
        double total = subtotal * (1.0 - discount);
        label_Subtotal.setText(String.format("Subtotal: %.2f", subtotal));
        label_Total.setText(String.format("Total: %.2f", total));

        // update DB: reduce quantity or set Soldout
        int newQty = currentQty - buyQty;
        String newStatus = newQty <= 0 ? "Soldout" : selected.getStatus();

        String sql = "UPDATE clothes SET product_Quantity=?, product_Status=? WHERE idClothes=?";
        Connection conn = DBConnect.connect();
        if (conn == null) { showAlert(Alert.AlertType.ERROR, "DB", "No connection"); return; }

        try {
            // Use transaction if you will do further writes like orders table
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, newQty);
                ps.setString(2, newQty <= 0 ? "Soldout" : selected.getStatus());
                ps.setInt(3, selected.getId());
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    conn.commit();
                    loadData();
                    showAlert(Alert.AlertType.INFORMATION, "Purchase", "Purchase completed. Total: " + String.format("%.2f", total));
                    tabPane.getSelectionModel().select(0); // go back to product tab
                } else {
                    conn.rollback();
                    showAlert(Alert.AlertType.ERROR, "Purchase", "Purchase failed.");
                }
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Purchase", "Error. See console.");
        } finally {
            try { conn.close(); } catch (Exception ignored) {}
        }
    }

    private void handleAdd() {
        // Basic validation
        String category = box_Cat.getValue();
        String name = input_Name.getText().trim();
        String type = input_Type.getText().trim();
        String color = input_Color.getText().trim();
        String size = input_Size.getText().trim();
        String condition = input_Cond.getText().trim();
        String quanText = input_Quan.getText().trim();
        String priceText = input_Price.getText().trim();

        if (category == null || category.isEmpty() || name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Category and Name are required.");
            return;
        }

        int quantity;
        double price;
        try {
            quantity = Integer.parseInt(quanText.isEmpty() ? "0" : quanText);
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Quantity must be an integer.");
            return;
        }

        try {
            price = Double.parseDouble(priceText.isEmpty() ? "0" : priceText);
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Price must be a number.");
            return;
        }

        // Insert into database
        String insertSql = "INSERT INTO clothes (product_Name, product_Category, product_Type, product_Color, product_Size, product_Quantity, product_Condition, product_Price, product_Status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = DBConnect.connect();
        if (conn == null) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not connect to DB. See console.");
            return;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, category);
            pstmt.setString(3, type);
            pstmt.setString(4, color);
            pstmt.setString(5, size);
            pstmt.setInt(6, quantity);
            pstmt.setString(7, condition);
            pstmt.setDouble(8, price);
            pstmt.setString(9, "Onstock");

            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                showAlert(Alert.AlertType.ERROR, "Insert Error", "No rows inserted.");
                return;
            }
            loadData();      // reload UI from DB
            handleReset();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Product added successfully.");

            // Get generated id if needed
            int newId = 0;
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) newId = keys.getInt(1);
            }


            // Optionally, instead of adding directly, you could call loadData() to reload everything:
            // loadData();
            //loadData();
            //handleReset();
            //showAlert(Alert.AlertType.INFORMATION, "Success", "Product added successfully.");

            // Clear fields after successful insert
            handleReset();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to insert data. See console.");
        } finally {
            try { conn.close(); } catch (Exception ignored) {}
        }


        // when user selects a row, populate the input fields
        table_List.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                box_Cat.setValue(newSel.getCategory());
                input_Name.setText(newSel.getName());
                input_Type.setText(newSel.getType());
                input_Color.setText(newSel.getColor());
                input_Size.setText(newSel.getSize());
                input_Cond.setText(newSel.getCondition());
                input_Quan.setText(String.valueOf(newSel.getQuantity()));
                input_Price.setText(String.valueOf(newSel.getPrice()));
            }
        });


    }

    private void handleReset() {
        box_Cat.setValue(null);
        input_Name.clear();
        input_Type.clear();
        input_Color.clear();
        input_Size.clear();
        input_Cond.clear();
        input_Quan.clear();
        input_Price.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}