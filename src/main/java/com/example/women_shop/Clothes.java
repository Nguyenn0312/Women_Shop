package com.example.women_shop;

import javafx.beans.property.*;

public class Clothes {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty category = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty color = new SimpleStringProperty();
    private final StringProperty size = new SimpleStringProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    private final StringProperty condition = new SimpleStringProperty();
    private final DoubleProperty price = new SimpleDoubleProperty();
    private final StringProperty status = new SimpleStringProperty();


    public Clothes(int id, String name, String category, String type, String color,
                   String size, int quantity, String condition, double price, String status) {

        this.id.set(id);
        this.name.set(name);
        this.category.set(category);
        this.type.set(type);
        this.color.set(color);
        this.size.set(size);
        this.quantity.set(quantity);
        this.condition.set(condition);
        this.price.set(price);
        this.status.set(status);
    }

    // ---- Property Getters ----
    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty categoryProperty() { return category; }
    public StringProperty typeProperty() { return type; }
    public StringProperty colorProperty() { return color; }
    public StringProperty sizeProperty() { return size; }
    public IntegerProperty quantityProperty() { return quantity; }
    public StringProperty conditionProperty() { return condition; }
    public DoubleProperty priceProperty() { return price; }
    public StringProperty statusProperty() { return status; }

    // ---- Value Getters (optional but recommended for TableView sorting) ----
    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getCategory() { return category.get(); }
    public String getType() { return type.get(); }
    public String getColor() { return color.get(); }
    public String getSize() { return size.get(); }
    public int getQuantity() { return quantity.get(); }
    public String getCondition() { return condition.get(); }
    public double getPrice() { return price.get(); }
    public String getStatus() { return status.get(); }
}
