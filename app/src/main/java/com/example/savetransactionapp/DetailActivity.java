package com.example.savetransactionapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.firestore.FirebaseFirestore;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Retrieve Data from Intent
        double amount = getIntent().getDoubleExtra("amount", 0);
        String date = getIntent().getStringExtra("date");
        String imagePath = getIntent().getStringExtra("imagePath");
        String docId = getIntent().getStringExtra("docId");
        String notes = getIntent().getStringExtra("notes");

        // Initialize Views
        TextView tvAmount = findViewById(R.id.tv_detail_amount);
        TextView tvDate = findViewById(R.id.tv_detail_date);
        TextView tvNotes = findViewById(R.id.tv_detail_notes);
        PhotoView imgStruk = findViewById(R.id.img_detail_struk);
        Button btnDelete = findViewById(R.id.btn_delete);

        // Set Data to Views
        tvDate.setText(date);
        tvAmount.setText("Rp " + String.format("%,.0f", amount));

        if (notes != null && !notes.isEmpty()) {
            tvNotes.setText(notes);
        } else {
            tvNotes.setText("-");
        }

        if (imagePath != null) {
            Glide.with(this).load(imagePath).into(imgStruk);
        }

        // Delete Logic
        btnDelete.setOnClickListener(v -> {
            if (docId != null) {
                FirebaseFirestore.getInstance()
                        .collection("transactions")
                        .document(docId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Transaksi Berhasil Dihapus", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Gagal menghapus data", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }
}