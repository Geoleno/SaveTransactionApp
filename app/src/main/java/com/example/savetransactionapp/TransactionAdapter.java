package com.example.savetransactionapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private Context context;
    private List<TransactionModel> transactionList;

    public TransactionAdapter(Context context, List<TransactionModel> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionModel transaction = transactionList.get(position);

        // Set Title/Notes
        String title = transaction.getNotes();
        if (title == null || title.isEmpty()) {
            title = "Transaksi Tanpa Catatan";
        }
        holder.tvTitle.setText(title);

        // Set Date & Amount
        holder.tvDate.setText(transaction.getDate());
        holder.tvAmount.setText("Rp " + String.format("%,.0f", transaction.getAmount()));

        // Load Image using Glide
        if (transaction.getImagePath() != null && !transaction.getImagePath().isEmpty()) {
            Glide.with(context)
                    .load(transaction.getImagePath())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.imgStruk);
        }

        // Handle Item Click (Open DetailActivity)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("amount", transaction.getAmount());
            intent.putExtra("date", transaction.getDate());
            intent.putExtra("imagePath", transaction.getImagePath());
            intent.putExtra("docId", transaction.getDocumentId());
            intent.putExtra("notes", transaction.getNotes());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAmount, tvDate;
        ImageView imgStruk;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDate = itemView.findViewById(R.id.tv_date);
            imgStruk = itemView.findViewById(R.id.img_struk);
        }
    }
}