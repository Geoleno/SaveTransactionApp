package com.example.savetransactionapp;

import java.util.Date;

public class TransactionModel {
    private String id;
    private String title;
    private double amount;
    private Date date;
    private String imageUrl;
    private String category;
    private String type;

    // 1. Constructor KOSONG (Wajib untuk Firebase)
    // Firebase butuh ini untuk membaca data
    public TransactionModel() {
    }

    // 2. Constructor Lengkap (Untuk memudahkan kita isi data nanti)
    public TransactionModel(String title, double amount, Date date, String imageUrl, String category, String type) {
        this.title = title;
        this.amount = amount;
        this.date = date;
        this.imageUrl = imageUrl;
        this.category = category;
        this.type = type;
    }

    // 3. Getter dan Setter (Wajib agar Firebase bisa ambil/taruh data)

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}