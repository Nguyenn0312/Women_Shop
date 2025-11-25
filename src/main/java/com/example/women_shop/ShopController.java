package com.example.women_shop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

import java.sql.SQLException;
import java.util.Optional;

public class ShopController {

    // Using DAO to isolate DB logic
    private final ProductDAO productDAO = new ProductDAO();
    private final ObservableList<Product> items = FXCollections.observableArrayList();

    // ================== FXML Controls ==================

    // Main containers and controls
    @FXML private ComboBox<String> box_Cat;
    @FXML private TabPane tabPane;
    @FXML private AnchorPane checkoutPane;

    // CRUD and Transaction Buttons
    @FXML private Button but_Add;
    @FXML private Button but_Reset;
    @FXML private Button but_Modify;
    @FXML private Button but_Delete;
    @FXML private Button but_Purchase;
    @FXML private Button but_Confirm;

    // Discount management buttons (Requirement 7)
    @FXML private Button but_ApplyDiscounts;
    @FXML private Button but_ClearDiscounts;

    // Input Fields for Add/Modify
    @FXML private TextField input_Name;
    @FXML private TextField input_PurchasePrice;
    @FXML private TextField input_SalePrice;
    @FXML private TextField input_Size;
    @FXML private TextField input_Quan;          // Stock (Read-only)

    // Controls for Checkout tab
    @FXML private Spinner<Integer> spin_PurchaseQuan;
    @FXML private ComboBox<String> box_Discount; // Discount for the transaction
    @FXML private ComboBox<String> box_TransactionType; // NEW: Buy/Sell Choice
    @FXML private Label label_Subtotal;
    @FXML private Label label_Total;

    // TableView Columns (must match Product properties)
    @FXML private TableView<Product> table_List;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, String> colSize;
    @FXML private TableColumn<Product, Integer> colQuan;
    @FXML private TableColumn<Product, Double> colSalePrice;
    @FXML private TableColumn<Product, Double> colDiscountPrice;
    @FXML private TableColumn<Product, String> colStatus;

    // Labels for statistics
    @FXML private Label labelInitialCapital;
    @FXML private Label labelCapital;
    @FXML private Label labelIncomes;
    @FXML private Label labelCosts;

    // ================== INITIALIZATION ==================

    @FXML
    public void initialize() {
        // Initialize Category ComboBox
        box_Cat.getItems().addAll("Clothing", "Shoes", "Accessory");

        // Initialize Spinner for quantity (Buy/Sell)
        spin_PurchaseQuan.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 1));

        // Initialize Discount ComboBox
        box_Discount.getItems().addAll("0%", "30%", "50%");
        box_Discount.getSelectionModel().selectFirst();

        // NEW: Initialize transaction type
        box_TransactionType.getItems().addAll("SELL", "BUY"); // Simple English terms
        box_TransactionType.getSelectionModel().selectFirst();

        // Setup cell value factories
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("sizeString"));
        colQuan.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colSalePrice.setCellValueFactory(new PropertyValueFactory<>("salePrice"));
        colDiscountPrice.setCellValueFactory(new PropertyValueFactory<>("discountPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        table_List.setItems(items);

        // 1. Initial data and stats loading
        loadData();
        refreshStats();

        // 2. Button Events
        but_Add.setOnAction(event -> handleAdd());
        but_Modify.setOnAction(event -> handleModify());
        but_Delete.setOnAction(event -> handleDelete());
        but_Reset.setOnAction(event -> handleReset());
        but_Confirm.setOnAction(event -> handleConfirm());
        but_Purchase.setOnAction(event -> handlePurchase());
        but_ApplyDiscounts.setOnAction(event -> handleApplyDiscounts());
        but_ClearDiscounts.setOnAction(event -> handleClearDiscounts());

        // 3. Row selection handler (for modify/transaction)
        table_List.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                fillForm(newSel);
                calculateSubTotal(newSel); // Recalculate totals for transaction
            } else {
                handleReset();
            }
        });

        // 4. Quantity/Discount/Type change handler in transaction tab
        spin_PurchaseQuan.valueProperty().addListener((obs, oldVal, newVal) -> {
            Product selected = table_List.getSelectionModel().getSelectedItem();
            if (selected != null) calculateSubTotal(selected);
        });
        box_Discount.valueProperty().addListener((obs, oldVal, newVal) -> {
            Product selected = table_List.getSelectionModel().getSelectedItem();
            if (selected != null) calculateSubTotal(selected);
        });
        box_TransactionType.valueProperty().addListener((obs, oldVal, newVal) -> {
            Product selected = table_List.getSelectionModel().getSelectedItem();
            if (selected != null) calculateSubTotal(selected);
        });


        // 5. Make quantity field non-editable
        input_Quan.setDisable(true);

        // Display initial capital
        labelInitialCapital.setText("Initial Capital: " + String.format("%.2f €", productDAO.getInitialCapital()));
    }

    // ================== DATA LOADING ==================

    private void loadData() {
        try {
            items.setAll(productDAO.loadAllProducts());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load products: " + e.getMessage());
        }
    }

    private void fillForm(Product p) {
        input_Name.setText(p.getName());
        input_PurchasePrice.setText(String.valueOf(p.getPurchasePrice()));
        input_SalePrice.setText(String.valueOf(p.getSalePrice()));
        input_Quan.setText(String.valueOf(p.getQuantity()));
        box_Cat.setValue(p.getCategory());

        // Size is specific to shoes and clothing
        if (p instanceof Clothing clothing) {
            input_Size.setText(String.valueOf(clothing.getSize()));
        } else if (p instanceof Shoes shoes) {
            input_Size.setText(String.valueOf(shoes.getSize()));
        } else {
            input_Size.setText("");
        }
    }

    private void handleReset() {
        input_Name.clear();
        input_PurchasePrice.clear();
        input_SalePrice.clear();
        input_Size.clear();
        input_Quan.clear();
        box_Cat.getSelectionModel().clearSelection();
        table_List.getSelectionModel().clearSelection();
    }

    // ================== CRUD (Requirements 4 & 12) ==================

    private void handleAdd() {
        try {
            String name = input_Name.getText();
            double purchasePrice = Double.parseDouble(input_PurchasePrice.getText());
            double salePrice = Double.parseDouble(input_SalePrice.getText());
            String category = box_Cat.getValue();
            String sizeInput = input_Size.getText();

            if (name.isEmpty() || category == null || input_PurchasePrice.getText().isEmpty() || input_SalePrice.getText().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Input Error", "Please fill all mandatory fields (Name, Prices, Category).");
                return;
            }

            // Create a temporary object to validate data according to model rules
            Product newProduct = switch (category) {
                case "Clothing" -> new Clothing(0, name, purchasePrice, salePrice, 0, 0, "Soldout", Integer.parseInt(sizeInput));
                case "Shoes" -> new Shoes(0, name, purchasePrice, salePrice, 0, 0, "Soldout", Integer.parseInt(sizeInput));
                case "Accessory" -> new Accessory(0, name, purchasePrice, salePrice, 0, 0, "Soldout");
                default -> throw new IllegalArgumentException("Unknown category.");
            };

            // The DAO inserts the product, assigns the ID and initial state (Stock 0, Status Soldout)
            productDAO.addProduct(newProduct, category, sizeInput);

            loadData();
            handleReset();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Product added successfully.");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Format Error", "Prices and size must be numbers.");
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", e.getMessage());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "DB Error", "Error while adding: " + e.getMessage());
        }
    }

    private void handleModify() {
        Product selected = table_List.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a product to modify.");
            return;
        }

        try {
            String name = input_Name.getText();
            double purchasePrice = Double.parseDouble(input_PurchasePrice.getText());
            double salePrice = Double.parseDouble(input_SalePrice.getText());
            String sizeInput = input_Size.getText();
            String category = selected.getCategory();

            // Update the selected Java object for validation
            selected.setName(name);
            selected.setPurchasePrice(purchasePrice);
            selected.setSalePrice(salePrice);

            if (selected instanceof Clothing clothing) {
                clothing.setSize(Integer.parseInt(sizeInput));
            } else if (selected instanceof Shoes shoes) {
                shoes.setSize(Integer.parseInt(sizeInput));
            }

            // The DAO updates the Name, Purchase Price, Sale Price and Size/Material fields in the DB
            productDAO.updateProduct(selected, category, sizeInput);

            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Product modified successfully.");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Format Error", "Prices and size must be numbers.");
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", e.getMessage());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "DB Error", "Error while modifying: " + e.getMessage());
        }
    }

    private void handleDelete() {
        Product selected = table_List.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a product to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deletion Confirmation");
        confirm.setHeaderText("Delete product " + selected.getName() + " ?");
        confirm.setContentText("This action is irreversible.");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // The DAO checks the stock before deleting (Requirement 3)
                productDAO.deleteProduct(selected.getId());
                loadData();
                handleReset();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Product deleted.");
            } catch (IllegalStateException e) {
                showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); // Stock > 0 message
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "DB Error", "Error while deleting: " + e.getMessage());
            }
        }
    }

    // ================== TRANSACTIONS (Requirement 6) ==================

    // Opens the transaction tab
    private void handlePurchase() {
        if (table_List.getSelectionModel().getSelectedItem() == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a product for the transaction.");
            return;
        }
        tabPane.getSelectionModel().select(1); // Selects the Checkout tab
    }

    // Calculates and displays subtotal and total in the transaction tab
    private void calculateSubTotal(Product p) {
        int quantity = spin_PurchaseQuan.getValue();
        String transactionType = box_TransactionType.getValue();

        double effectivePrice;
        if (transactionType != null && transactionType.startsWith("BUY")) {
            // For purchase (BUY), use the purchase price
            effectivePrice = p.getPurchasePrice();
        } else {
            // For sale (SELL), use the effective sale price (with fixed discount if > 0)
            effectivePrice = p.getEffectiveSalePrice();

            // Apply manual discount (for the exercise)
            double reduction = switch (box_Discount.getValue()) {
                case "30%" -> 0.70;
                case "50%" -> 0.50;
                default -> 1.00;
            };
            effectivePrice *= reduction;
        }

        double total = effectivePrice * quantity;

        label_Subtotal.setText(String.format("Unit Price: %.2f €", effectivePrice));
        label_Total.setText(String.format("Total: %.2f €", total));
    }

    // Confirms BUY or SELL
    private void handleConfirm() {
        Product selected = table_List.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String transactionType = box_TransactionType.getValue();
        if (transactionType == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please choose the transaction type (Buy or Sell).");
            return;
        }

        int quantity = spin_PurchaseQuan.getValue();
        if (quantity <= 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid Quantity", "Quantity must be greater than zero.");
            return;
        }

        try {
            // Price and manual discount logic
            double buyPrice = selected.getPurchasePrice();
            double sellPrice = selected.getEffectiveSalePrice() * switch (box_Discount.getValue()) {
                case "30%" -> 0.70;
                case "50%" -> 0.50;
                default -> 1.00;
            };

            if (transactionType.startsWith("BUY")) {
                // BUY LOGIC - Requirement 6 & 5
                double totalCost = buyPrice * quantity;
                double income = productDAO.calculateIncome();
                double cost = productDAO.calculateCost();
                double currentCapital = productDAO.calculateCapital(income, cost);

                // Requirement 5: Budget check
                if (totalCost > currentCapital) {
                    showAlert(Alert.AlertType.ERROR, "Budget Error (Requirement 5)", "Insufficient funds. Total cost: " + String.format("%.2f €", totalCost) + " > Current capital: " + String.format("%.2f €", currentCapital));
                    return;
                }

                productDAO.recordTransaction(selected.getId(), "BUY", quantity, buyPrice);
                showAlert(Alert.AlertType.INFORMATION, "Purchase", "Purchase successful. Cost: " + String.format("%.2f €", totalCost));

            } else if (transactionType.startsWith("SELL")) {
                // SELL LOGIC - Requirement 6
                // DAO and Trigger handle stock check
                productDAO.recordTransaction(selected.getId(), "SELL", quantity, sellPrice);
                showAlert(Alert.AlertType.INFORMATION, "Sale", "Sale successful. Income: " + String.format("%.2f €", sellPrice * quantity));
            }

        } catch (SQLException e) {
            // DAO or Trigger returns stock error for sale
            showAlert(Alert.AlertType.ERROR, "Transaction Error", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "System Error", "An unexpected error occurred: " + e.getMessage());
        }

        loadData();
        refreshStats();
        tabPane.getSelectionModel().select(0); // Back to main tab
    }

    // ================== DISCOUNT MANAGEMENT (Requirement 7) ==================

    private void handleApplyDiscounts() {
        try {
            // Fixed discount: 30% Clothing (0.70), 20% Shoes (0.80), 50% Accessory (0.50)
            productDAO.setDiscountPriceByCategory("Clothing", 0.70); // 30% off -> 70% price
            productDAO.setDiscountPriceByCategory("Shoes", 0.80);    // 20% off -> 80% price
            productDAO.setDiscountPriceByCategory("Accessory", 0.50); // 50% off -> 50% price

            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Discounts applied according to requirements.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "DB Error", "Error applying discounts: " + e.getMessage());
        }
    }

    private void handleClearDiscounts() {
        try {
            productDAO.clearAllDiscountPrices();
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Success", "All discounts have been cleared.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "DB Error", "Error clearing discounts: " + e.getMessage());
        }
    }

    // ================== STATISTICS (Requirement 5) ==================

    @FXML
    public void refreshStats() {
        try {
            double income = productDAO.calculateIncome();
            double cost = productDAO.calculateCost();
            double capital = productDAO.calculateCapital(income, cost);

            labelCapital.setText("Current Capital: " + String.format("%.2f €", capital));
            labelIncomes.setText("Income: " + String.format("%.2f €", income));
            labelCosts.setText("Costs: " + String.format("%.2f €", cost));

            // Update capital color for aesthetics
            if (capital < productDAO.getInitialCapital()) {
                labelCapital.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #FF5733;"); // Red
            } else {
                labelCapital.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #00FF00;"); // Green
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to calculate statistics.");
        }
    }

    // ================== UTILITIES ==================

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}