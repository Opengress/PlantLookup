package net.opengress.plantlookup;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.opengress.plantlookup.databinding.FragmentDetailBinding;

public class DetailFragment extends Fragment {

    private FragmentDetailBinding binding;

    @SuppressLint("Range")
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Retrieve data from the arguments bundle
        Bundle arguments = getArguments();
        if (arguments == null) {
            throw new RuntimeException("How did I reach DetailFragment without touching SearchFragment?");
        }

        String plantNVSCode = arguments.getString("plantNVSCode");

        SQLiteDatabase db = DatabaseManager.getDatabase();

        // NB it may be wiser to do a second query and get the names as a list rather than this ugly thing
        String[] selectionArgs = {plantNVSCode};
        Cursor cursor = db.rawQuery("SELECT Plants.NVSCode, Plants.TaxonID, Plants.SpeciesName, " +
                "Plants.Family, Plants.Genus, Plants.Species, TBioStatus.Description AS BioStatus, " +
                "TPlantType.Description AS PlantType, TGrowthForm.Description AS GrowthForm, " +
                "TThreatenedStatus.Description AS ThreatenedStatus, " +
                "GROUP_CONCAT(Aliases.AliasName, ', ') AS Names\n" +
                "FROM Plants\n" +
                "LEFT JOIN TBioStatus ON Plants.BioStatusID = TBioStatus.ID\n" +
                "LEFT JOIN TPlantType ON Plants.PlantTypeID = TPlantType.ID\n" +
                "LEFT JOIN TGrowthForm ON Plants.GrowthFormID = TGrowthForm.ID\n" +
                "LEFT JOIN TThreatenedStatus ON Plants.ThreatenedStatusID = TThreatenedStatus.ID\n" +
                "LEFT JOIN Aliases ON Plants.NVSCode = Aliases.PlantNVSCode\n" +
                "WHERE Plants.NVSCode = ? LIMIT 1", selectionArgs);

        cursor.moveToFirst();
        String[] plant = new String[]{
                cursor.getString(cursor.getColumnIndex("NVSCode")),
                String.valueOf(cursor.getInt(cursor.getColumnIndex("TaxonID"))),
                cursor.getString(cursor.getColumnIndex("SpeciesName")),
                cursor.getString(cursor.getColumnIndex("Family")),
                cursor.getString(cursor.getColumnIndex("Genus")),
                cursor.getString(cursor.getColumnIndex("Species")),
                cursor.getString(cursor.getColumnIndex("BioStatus")),
                cursor.getString(cursor.getColumnIndex("PlantType")),
                cursor.getString(cursor.getColumnIndex("GrowthForm")),
                cursor.getString(cursor.getColumnIndex("ThreatenedStatus")),
                cursor.getString(cursor.getColumnIndex("Names")),
        };

        cursor.close();

        binding = FragmentDetailBinding.inflate(inflater, container, false);

        binding.titleText.setText(plant[2]);
        binding.nvsField.setText(plant[0]);
//        binding.speciesName.setText(plant[2]);
        binding.familyField.setText(plant[3]);
        binding.genusField.setText(plant[4]);
        binding.speciesField.setText(plant[5]);
        binding.floraCategoryField.setText(String.format("%s–%s", plant[7], plant[6]));
        binding.growthFormField.setText(plant[8]);
        binding.threatenedStatusField.setText(plant[9]);
        binding.synonymsField.setText(String.format("Could be called: %s", plant[10]));

        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}