package net.opengress.plantlookup;

import static android.database.sqlite.SQLiteDatabase.*;
import static net.opengress.plantlookup.utils.FileUtils.copy;
import static net.opengress.plantlookup.utils.SetupUtils.*;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private static SQLiteDatabase db;
    private static File file;
    public static final String fileName = "plantNames.db";
    private static int mode = OPEN_READONLY;

    public static synchronized SQLiteDatabase getDatabase() {
        db = getDatabase(false);
        return db;
    }

    public static synchronized SQLiteDatabase getDatabase(boolean writable) {

        int mode = writable ? OPEN_READWRITE : OPEN_READONLY;

        if (db != null && db.isOpen()) {
            if (mode != DatabaseManager.mode) {
                db.close();
            } else {
                return db;
            }
        }

        DatabaseManager.mode = mode;
        db = openDatabase(getFile().getPath(), null, mode);

        return db;
    }

    public static synchronized File getFile() {
        if (file == null) {
            file = Application.getInstance().getDatabasePath(fileName);
        }
        return file;
    }

    @NonNull
    @Contract(" -> new")
    public static Date getLastUpdateTime() {
        return new Date(getFile().lastModified());
    }

    public static synchronized void createDatabase() {
        File dbFile = getFile();
        if (dbFile.exists()) {
            return; // Database already exists, no need to create it
        }

        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

        try {
            executeSQLScript(db);
        } catch (IOException e) {
            Log.e("DATABASE", "Failed to execute database setup script: " + e.getMessage());
        }

        db.close();
    }

    private static void executeSQLScript(SQLiteDatabase db) throws IOException {
        InputStream is = Application.getInstance().getAssets().open("plantNames.db.sql");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        StringBuilder statement = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("--")) {
                continue; // Skip empty lines and comments
            }
            statement.append(line).append(" ");
            if (line.endsWith(";")) { // Execute when reaching a full statement
                db.execSQL(statement.toString());
                statement.setLength(0); // Clear buffer
            }
        }
        reader.close();
        is.close();
    }


    public static synchronized void populateDatabase(List <Map<String, String>> preferredIDs, Map<String, List<String>> synonyms) {
        Log.d("DATABASE", "Creating database.");
        File dbFile = getFile();
        String path = dbFile.getAbsolutePath();
        String backupPath = path + ".bak." + System.currentTimeMillis();

        // Backup database
        try {
            copy(path, backupPath);
        } catch (IOException e) {
            // we just continue if it fails - only likely failure is that it hasn't been created yet
            Log.w("DATABASE", "Failed to create database backup: " + e.getMessage());
        }

        if (dbFile.exists() && !dbFile.delete()) {
            throw new RuntimeException("Can't delete existing database file");
        }

        createDatabase();

        SQLiteDatabase db = getDatabase(true);
        db.beginTransaction(); // Start transaction

        try {
            // Insert preferred plant data
            try (android.database.sqlite.SQLiteStatement idStmt = db.compileStatement(
                    "INSERT OR REPLACE INTO Plants (NVSCode, TaxonID, SpeciesName, Family, Genus, Species, BioStatusID, PlantTypeId, GrowthFormID, ThreatenedStatusID) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                for (Map<String, String> plant : preferredIDs) {
                    idStmt.bindString(1, plant.get(NVSCode));
                    idStmt.bindLong(2, Long.parseLong(plant.get(TaxonID)));
                    idStmt.bindString(3, plant.get(SpeciesName));
                    idStmt.bindString(4, plant.get(Family));
                    idStmt.bindString(5, plant.get(Genus));
                    idStmt.bindString(6, plant.get(Species));
                    idStmt.bindLong(7, Long.parseLong(plant.get(BioStatusID)));
                    idStmt.bindLong(8, Long.parseLong(plant.get(PlantTypeID)));
                    idStmt.bindLong(9, Long.parseLong(plant.get(GrowthFormID)));
                    idStmt.bindLong(10, Long.parseLong(plant.get(ThreatenedStatusID)));
                    idStmt.executeInsert();
                }
            }

            // Insert synonyms
            try (android.database.sqlite.SQLiteStatement namesStmt = db.compileStatement(
                    "INSERT OR IGNORE INTO Aliases (PlantNVSCode, AliasName) VALUES (?, ?)")) {

                for (Map.Entry<String, List<String>> entry : synonyms.entrySet()) {
                    String plantId = entry.getKey();
                    for (String name : entry.getValue()) {
                        namesStmt.bindString(1, plantId);
                        namesStmt.bindString(2, name);
                        namesStmt.executeInsert();

                        // Normalize and insert if different
                        String cleanedName = clean(name);
                        if (!name.equals(cleanedName)) {
                            namesStmt.bindString(1, plantId);
                            namesStmt.bindString(2, cleanedName);
                            namesStmt.executeInsert();
                        }
                    }
                }
            }

            db.setTransactionSuccessful();

            File backupFile = new File(backupPath);
            if (backupFile.exists() && !backupFile.delete()) {
                throw new RuntimeException("Can't delete backup database file");
            }

            Log.d("DATABASE", "Database creation completed successfully.");

        } catch (Exception e) {
            Log.e("DATABASE", "Database error: " + e.getMessage());
            try {
                copy(backupPath, path);
            } catch (IOException e2) {
                Log.e("DATABASE", "Failed to restore database backup: " + e2.getMessage());
            }
        } finally {
            db.endTransaction();
        }
    }

}