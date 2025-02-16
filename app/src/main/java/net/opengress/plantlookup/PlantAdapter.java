package net.opengress.plantlookup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.TextView;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** @noinspection unchecked, ClassEscapesDefinedScope */
public class PlantAdapter extends RecyclerView.Adapter<PlantAdapter.PlantViewHolder> {
    private List<Pair<String,String>> originalData;
    private List<Pair<String,String>> filteredData;
    private CustomFilter filter;
    private final OnPlantItemClickListener itemClickListener;

    public PlantAdapter(Context context, List<Pair<String,String>> data, OnPlantItemClickListener itemClickListener) {
        this.originalData = data;
        this.filteredData = new ArrayList<>(originalData);
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new PlantViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(PlantViewHolder holder, int position) {
        Pair<String,String> plant = filteredData.get(position);
        holder.itemView.setOnClickListener(view -> {
            if (itemClickListener != null) {
                itemClickListener.onPlantItemClick(plant.first);
            }
        });
        holder.speciesNameTextView.setText(plant.second);
    }

    @Override
    public int getItemCount() {
        return filteredData.size();
    }

    public Filter getFilter() {
        if (filter == null) {
            filter = new CustomFilter();
        }
        return filter;
    }

    private class CustomFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            List<Pair<String, String>> filteredList = new ArrayList<>();

            if (originalData == null) {
                originalData = new ArrayList<>(filteredData); // Backup original data
            }

            if (constraint == null || constraint.length() == 0) {
                // No filter applied; return the full list
                filteredList.addAll(originalData);
            } else {
                // Apply filtering logic based on the constraint (search query)
                String query = constraint.toString();

                SQLiteDatabase db = DatabaseManager.getDatabase();
                // can add more clauses in here to filter out exotics etc
//                String selection = "(AliasName LIKE ? OR PlantNVSCode LIKE ?) AND TBioStatus.Description != 'Exotic'";
                // searching for NVSCodes here allows us to quickly grab synonyms in a dirty way
                String selection = "AliasName LIKE ? OR PlantNVSCode LIKE ?";
                String[] selectionArgs = { "%" + query + "%", "%" + query + "%" };

                // can put a join in here
//                Cursor cursor = db.query("Aliases LEFT JOIN Plants ON Plants.NVSCode = Aliases.PlantNVSCode LEFT JOIN TBioStatus ON Plants.BioStatusID = TBioStatus.ID", null, selection, selectionArgs, null, null, null);
                Cursor cursor = db.query("Aliases", null, selection, selectionArgs, null, null, "Aliases.PlantNVSCode, Aliases.AliasName");
                while (cursor.moveToNext()) {
                    // Create DataItem objects from cursor data
                    @SuppressLint("Range") String plantType = cursor.getString(cursor.getColumnIndex("PlantNVSCode"));
                    @SuppressLint("Range") String speciesName = cursor.getString(cursor.getColumnIndex("AliasName"));
                    Pair<String, String> item = new Pair<>(plantType, speciesName);
                    // Populate item with cursor data
                    filteredList.add(item);
                }
                cursor.close();
            }

            results.count = filteredList.size();
            results.values = filteredList;
            return results;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredData = (List<Pair<String, String>>) results.values;
            notifyDataSetChanged();
        }
    }

    static class PlantViewHolder extends RecyclerView.ViewHolder {
        final TextView speciesNameTextView;

        public PlantViewHolder(View itemView) {
            super(itemView);
            speciesNameTextView = itemView.findViewById(android.R.id.text1);
        }
    }
}
