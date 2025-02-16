package net.opengress.plantlookup;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import net.opengress.plantlookup.databinding.FragmentSearchBinding;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment implements OnPlantItemClickListener {

    private FragmentSearchBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SQLiteDatabase db = DatabaseManager.getDatabase();

        // we can filter out exotics like this
//        Cursor cursor = db.rawQuery("SELECT Aliases.PlantNVSCode, Aliases.AliasName FROM Aliases LEFT JOIN Plants ON Plants.NVSCode = Aliases.PlantNVSCode LEFT JOIN TBioStatus ON Plants.BioStatusID = TBioStatus.ID WHERE TBioStatus.Description != 'Exotic'", null);
        Cursor cursor = db.rawQuery("SELECT Aliases.PlantNVSCode, Aliases.AliasName FROM Aliases ORDER BY Aliases.PlantNVSCode, Aliases.AliasName", null);
        List<Pair<String, String>> plantList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String PlantNVSCode = cursor.getString(cursor.getColumnIndex("PlantNVSCode"));
                @SuppressLint("Range") String AliasName = cursor.getString(cursor.getColumnIndex("AliasName"));

                Pair<String, String> plant = new Pair<>(PlantNVSCode, AliasName);
                plantList.add(plant);
            } while (cursor.moveToNext());
        }
        cursor.close();
        PlantAdapter adapter = new PlantAdapter(getContext(), plantList, this);
        binding.searchResult.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.searchResult.setAdapter(adapter);

        binding.plantSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                // really no point in this doing anything at the moment
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                adapter.getFilter().filter(s);
                binding.searchResult.scrollBy(0, 0);
                return false;
            }
        });
        binding.plantSearch.setFocusable(true);
        binding.plantSearch.requestFocus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onPlantItemClick(String plantNVSCode) {
        // Create a bundle to pass data to the second fragment
        Bundle bundle = new Bundle();
        bundle.putString("plantNVSCode", plantNVSCode);

        NavHostFragment.findNavController(SearchFragment.this)
                .navigate(R.id.action_SearchFragment_to_DetailFragment, bundle);
    }

}