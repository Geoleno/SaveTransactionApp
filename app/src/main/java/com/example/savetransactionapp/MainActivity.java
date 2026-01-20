package com.example.savetransactionapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvTotalExpense;
    private Button btnFilter;
    private RecyclerView rvGallery;
    private FloatingActionButton fabScan;

    private TransactionAdapter adapter;
    private List<TransactionModel> allTransactionList;
    private List<TransactionModel> displayedList;

    private FirebaseFirestore db;
    private SimpleDateFormat sdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI Components
        tvTotalExpense = findViewById(R.id.tv_total_expense);
        btnFilter = findViewById(R.id.btn_filter);
        rvGallery = findViewById(R.id.rv_gallery);
        fabScan = findViewById(R.id.fab_scan);

        // Date format must match the database format
        sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Setup RecyclerView
        allTransactionList = new ArrayList<>();
        displayedList = new ArrayList<>();
        adapter = new TransactionAdapter(this, displayedList);
        rvGallery.setLayoutManager(new GridLayoutManager(this, 2));
        rvGallery.setAdapter(adapter);

        // Setup Firestore
        db = FirebaseFirestore.getInstance();
        loadTransactions();

        // Set Listeners
        fabScan.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ScanActivity.class)));
        btnFilter.setOnClickListener(v -> showFilterMenu());
    }

    private void loadTransactions() {
        db.collection("transactions")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        allTransactionList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            TransactionModel item = doc.toObject(TransactionModel.class);
                            item.setDocumentId(doc.getId());
                            allTransactionList.add(item);
                        }
                        // Default filter: Current Month
                        filterTransactions("Bulan Ini");
                    }
                });
    }

    private void showFilterMenu() {
        PopupMenu popup = new PopupMenu(this, btnFilter);
        popup.getMenu().add("Semua Waktu");
        popup.getMenu().add("Bulan Ini");
        popup.getMenu().add("Bulan Lalu");
        popup.getMenu().add("Pilih Tanggal (Custom)");

        popup.setOnMenuItemClickListener(item -> {
            String choice = item.getTitle().toString();
            if (choice.equals("Pilih Tanggal (Custom)")) {
                showCustomDatePicker();
            } else {
                btnFilter.setText(choice + " ▼");
                filterTransactions(choice);
            }
            return true;
        });
        popup.show();
    }

    private void showCustomDatePicker() {
        MaterialDatePicker<Pair<Long, Long>> materialDatePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Pilih Rentang Tanggal")
                .setSelection(Pair.create(MaterialDatePicker.thisMonthInUtcMilliseconds(), MaterialDatePicker.todayInUtcMilliseconds()))
                .build();

        materialDatePicker.addOnPositiveButtonClickListener(selection -> {
            String startDate = sdf.format(new Date(selection.first));
            String endDate = sdf.format(new Date(selection.second));
            btnFilter.setText(startDate + " - " + endDate);
            filterByCustomRange(selection.first, selection.second);
        });

        materialDatePicker.show(getSupportFragmentManager(), "TAG");
    }

    private void filterTransactions(String type) {
        displayedList.clear();
        Calendar cal = Calendar.getInstance();
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());

        cal.add(Calendar.MONTH, -1);
        String lastMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());

        double totalExpense = 0;

        for (TransactionModel item : allTransactionList) {
            boolean include = false;
            if (type.equals("Semua Waktu")) include = true;
            else if (type.equals("Bulan Ini") && item.getDate().contains(currentMonth)) include = true;
            else if (type.equals("Bulan Lalu") && item.getDate().contains(lastMonth)) include = true;

            if (include) {
                displayedList.add(item);
                totalExpense += item.getAmount();
            }
        }
        updateUI(totalExpense);
    }

    private void filterByCustomRange(Long startMillis, Long endMillis) {
        displayedList.clear();
        double totalExpense = 0;

        for (TransactionModel item : allTransactionList) {
            try {
                Date dateStruk = sdf.parse(item.getDate());
                if (dateStruk != null) {
                    long strukMillis = dateStruk.getTime();
                    if (strukMillis >= startMillis && strukMillis <= endMillis) {
                        displayedList.add(item);
                        totalExpense += item.getAmount();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        updateUI(totalExpense);
    }

    private void updateUI(double total) {
        adapter.notifyDataSetChanged();
        tvTotalExpense.setText("Rp " + String.format("%,.0f", total));
    }
}