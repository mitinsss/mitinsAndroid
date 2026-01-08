package com.example.weighttracker;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class WeightAdapter extends RecyclerView.Adapter<WeightAdapter.WeightViewHolder> {

    private final List<WeightEntry> entryList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(WeightEntry entry);
    }

    public WeightAdapter(List<WeightEntry> entryList) {
        this.entryList = entryList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public WeightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weight, parent, false);
        return new WeightViewHolder(view);
    }

    // Sasaista datus ar skatu un iestata klausītāju
    @Override
    public void onBindViewHolder(@NonNull WeightViewHolder holder, int position) {
        WeightEntry entry = entryList.get(position);

        holder.tvDate.setText(String.format("Datums: %s", entry.iegutDatumu()));
        holder.tvWeight.setText(String.format("Svars: %s kg", entry.iegutSvaru()));
        holder.tvBMI.setText(String.format("ĶMI: %.2f", entry.iegutKmi()));

        if (entry.iegutFotoCelu() != null && !entry.iegutFotoCelu().isEmpty()) {
            File imgFile = new File(entry.iegutFotoCelu());
            if (imgFile.exists()) {
                holder.ivThumbnail.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
            } else {
                holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(entry);
            }
        });
    }

    // Atgriež kopējo ierakstu skaitu sarakstā
    @Override
    public int getItemCount() {
        return entryList.size();
    }

    static class WeightViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvWeight, tvBMI;
        ImageView ivThumbnail;

        public WeightViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvBMI = itemView.findViewById(R.id.tvBMI);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
        }
    }
}
