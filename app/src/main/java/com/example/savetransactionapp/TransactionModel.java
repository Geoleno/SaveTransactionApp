package com.example.savetransactionapp;

public class TransactionModel {
    private String documentId;
    private double amount;
    private String date;
    private String imagePath;
    private String notes;

    // Empty constructor required for Firestore
    public TransactionModel() {}

    // Full constructor
    public TransactionModel(double amount, String date, String imagePath, String notes) {
        this.amount = amount;
        this.date = date;
        this.imagePath = imagePath;
        this.notes = notes;
    }

    // Getters
    public double getAmount() { return amount; }
    public String getDate() { return date; }
    public String getImagePath() { return imagePath; }
    public String getNotes() { return notes; }

    // Document ID handling
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
}