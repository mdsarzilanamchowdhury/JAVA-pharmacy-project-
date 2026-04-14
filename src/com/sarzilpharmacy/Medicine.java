package com.sarzilpharmacy;

public class Medicine {
    private String name;
    private double price;
    private int quantity;
    private double discount;
    
    // Constructor
    public Medicine(String name, double price, int quantity, double discount) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.discount = discount;
    }
    
    // Getters
    public String getName() { 
        return name; 
    }
    
    public double getPrice() { 
        return price; 
    }
    
    public int getQuantity() { 
        return quantity; 
    }
    
    public double getDiscount() { 
        return discount; 
    }
    
    // Calculate total price
    public double getTotalPrice() { 
        return price * quantity; 
    }
    
    // Calculate net price after discount
    public double getNetPrice() { 
        return getTotalPrice() - discount; 
    }
    
    // Setters
    public void setName(String name) { 
        this.name = name; 
    }
    
    public void setPrice(double price) { 
        this.price = price; 
    }
    
    public void setQuantity(int quantity) { 
        this.quantity = quantity; 
    }
    
    public void setDiscount(double discount) { 
        this.discount = discount; 
    }
    
    @Override
    public String toString() {
        return String.format("Medicine{name='%s', price=%.2f, quantity=%d, discount=%.2f}", 
                           name, price, quantity, discount);
    }
}