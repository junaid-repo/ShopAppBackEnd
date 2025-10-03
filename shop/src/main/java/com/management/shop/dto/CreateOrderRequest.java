package com.management.shop.dto;

public class CreateOrderRequest {

    private Integer amount;
    private String currency;

    // Default no-argument constructor (required by Jackson for deserialization)
    public CreateOrderRequest() {
    }

    // Optional: Constructor with arguments for easier object creation in tests
    public CreateOrderRequest(Integer amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    // --- Getters and Setters ---

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    // Optional: toString() method for easy logging and debugging
    @Override
    public String toString() {
        return "CreateOrderRequest{" +
                "amount=" + amount +
                ", currency='" + currency + '\'' +
                '}';
    }
}