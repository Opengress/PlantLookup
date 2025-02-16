package net.opengress.plantlookup.utils;

import static net.opengress.plantlookup.DatabaseManager.populateDatabase;
import static net.opengress.plantlookup.utils.FileUtils.clearCache;
import static net.opengress.plantlookup.utils.FileUtils.downloadSync;
import static net.opengress.plantlookup.utils.FileUtils.readSync;
import static net.opengress.plantlookup.utils.FileUtils.saveToFile;
import static java.lang.String.valueOf;
import static java.text.Normalizer.*;
import static java.util.Objects.requireNonNull;

import android.app.Activity;
import android.util.Log;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import net.opengress.plantlookup.Application;
import net.opengress.plantlookup.R;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SetupUtils {

    private enum types {
        vascular(1),
        nonvascular(2);

        private final int value;

        types(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private enum biostatuses {
        absent(1),
        exotic(2),
        indigenousendemic(3),
        indigenousnonendemic(4),
        indigenousunspecified(5),
        uncertain(6),
        unknown(7);

        private final int value;

        biostatuses(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private enum growthforms {
        fern(1),
        forb(2),
        graminoid(3),
        grasstree(4),
        herbaceousmixed(5),
        mistletoe(6),
        mixed(7),
        nonvascular(8),
        palm(9),
        shrub(10),
        subshrub(11),
        tree(12),
        treefern(13),
        unknown(14),
        vine(15),
        woodymixed(16);

        private final int value;

        growthforms(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private enum threatenedstatuses {
        coloniser(1),
        datadeficient(2),
        declining(3),
        nationallycritical(4),
        nationallyendangered(5),
        nationallyvulnerable(6),
        naturallyuncommon(7),
        notthreatened(8),
        notthreatenedassumed(9),
        recovering(10),
        relict(11),
        taxonomicallyindistinct(12),
        unknown(13),
        vagrant(14);

        private final int value;

        threatenedstatuses(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public interface iProgressCallback {
        void onProgressUpdate(int current);
        void init(int totalRecords);
        void finish();
    }

    public static final String NVSCode = "NVSCode";
   public static final String TaxonID = "TaxonID";
   public static final String SpeciesName = "SpeciesName";
   public static final String Family = "Family";
   public static final String Genus = "Genus";
   public static final String Species = "Species";
   public static final String BioStatus = "BioStatus";
   public static final String PlantType = "PlantType";
   public static final String Palatibility = "Palatibility";
   public static final String GrowthForm = "GrowthForm";
   public static final String PreferredTaxonID = "PreferredTaxonID";
   public static final String PreferredCode = "PreferredCode";
   public static final String PreferredName = "PreferredName";
   public static final String VernacularNames = "VernacularNames";
   public static final String ThreatenedStatus = "ThreatenedStatus";
   public static final String BioStatusID = "BioStatusID";
   public static final String PlantTypeID = "PlantTypeID";
   public static final String GrowthFormID = "GrowthFormID";
   public static final String ThreatenedStatusID = "ThreatenedStatusID";

   public static final File csvFile = new File(Application.getInstance().getCacheDir(),"CurrentNVSNames.csv");


    private static final Map<String, List<String>> synonyms = new HashMap<>();
    private static final List<Map<String, String>> preferredIDs = new ArrayList<>();

    public static void processSynonyms(@NonNull Map<String, String> d) {

        List<String> newSynonyms = new ArrayList<>();
        newSynonyms.add(d.get(SpeciesName));
        newSynonyms.add(d.get(NVSCode));

        // I swear android studio is completely stupid
        if (d.get(VernacularNames) != null && !d.get(VernacularNames).isEmpty()) {
            newSynonyms.addAll(Arrays.asList(d.get(VernacularNames).split(";")));
        }

        // sugared with streams and maps and whatnot
        List<String> cleanedSynonyms = new ArrayList<>();
        for (String s : newSynonyms) {
            if (s != null) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    cleanedSynonyms.add(trimmed);
                }
            }
        }

        // getOrDefault
        List<String> existingSynonyms = synonyms.get(d.get(PreferredCode));
        if (existingSynonyms == null) {
            existingSynonyms = new ArrayList<>();
            synonyms.put(d.get(PreferredCode), existingSynonyms);
        }

        existingSynonyms.addAll(cleanedSynonyms);

        // Remove duplicates
        List<String> uniqueSynonyms = new ArrayList<>(new LinkedHashSet<>(existingSynonyms));

        synonyms.put(d.get(PreferredCode), uniqueSynonyms);
    }

    private static void downloadCSVFile() throws IOException {
        Log.d("SETUP", "Downloading Current NVS Names CSV...");
        saveToFile(csvFile, downloadSync("https://nvs.landcareresearch.co.nz/Content/CurrentNVSNames.csv"));
    }

    /**
     * Creates a reader capable of handling BOMs.
     *
     * @param path The path to read.
     * @return a new InputStreamReader for UTF-8 bytes.
     * @throws IOException if an I/O error occurs.
     */
    private static InputStreamReader newReader(final File path) throws IOException {
        return new InputStreamReader(BOMInputStream.builder()
                .setPath(path.getPath())
                .get(), "UTF-8");
    }

    private static Iterable<CSVRecord> getRecords(Reader reader) throws IOException {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get().parse(reader);
    }

    private static void readCSV(iProgressCallback callback) {
        Log.d("SETUP", "Parsing NVS Names CSV file...");
        int totalRecords = 0;
        int currentRecord = 0;
        try (Reader reader = newReader(csvFile)) {
            Iterable<CSVRecord> records = getRecords(reader);
            for (CSVRecord ignored : records) {
                ++totalRecords;
            }
            if (callback != null) {
                callback.init(totalRecords);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        try (Reader reader = newReader(csvFile)) {
            Iterable<CSVRecord> records = getRecords(reader);
            for (CSVRecord record : records) {

                Map<String, String> d = record.toMap();
                if (requireNonNull(d.get(VernacularNames)).contains("?")) {
                    d.put(VernacularNames, fixVernacularNames(d.get(TaxonID)));
                }

                if (Objects.equals(d.get(TaxonID), d.get(PreferredTaxonID))) {
                    preferredIDs.add(Map.of(
                            NVSCode, requireNonNull(d.get(NVSCode)),
                            TaxonID, requireNonNull(d.get(TaxonID)),
                            SpeciesName, requireNonNull(d.get(SpeciesName)),
                            Family, requireNonNull(d.get(Family)),
                            Genus, requireNonNull(d.get(Genus)),
                            Species, requireNonNull(d.get(Species)),
                            BioStatusID, valueOf(biostatuses.valueOf(norm(d.get(BioStatus))).getValue()),
                            PlantTypeID, valueOf(types.valueOf(norm(d.get(PlantType))).getValue()),
                            GrowthFormID, valueOf(growthforms.valueOf(norm(d.get(GrowthForm))).getValue()),
                            ThreatenedStatusID, valueOf(threatenedstatuses.valueOf(norm(d.get(ThreatenedStatus))).getValue())
                    ));
                }

                processSynonyms(d);

                if (callback != null) {
                    callback.onProgressUpdate(++currentRecord);
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    // reduces a string to a pure lowercase alphabetic representation to normalize lookups in array keys
    @NonNull
    private static String norm(@NonNull String s) {
        return s.toLowerCase().replaceAll("\\W", "");
    }

    // for when there's a "?" in the name and you want the proper string with macron. fetch from nvs
    @NonNull
    private static String fixVernacularNames(@NonNull String id) {
        Log.d("SETUP", "Fixing vernacular name for "+id+"...");
        try {
            File cacheFile = new File(Application.getInstance().getCacheDir(), "nvs_" + id);
            String json;
            if (cacheFile.exists()) {
                Log.d("SETUP", "Cache hit!");
                json = readSync(cacheFile.getAbsolutePath());
            } else {
                Log.d("SETUP", "Cache miss - querying web API...");
                json = downloadSync("https://api-web-nvs.landcareresearch.co.nz/api/authority/taxons/" + id);
                saveToFile(cacheFile, json);
                try {
                    // was a 1-second sleep. But consumers can't wait that long.
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
            }
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.optString(VernacularNames, "");
        } catch (Throwable e) {
            Log.d("SETUP", "Something went wrong while fetching vernacular name!");
            return "";
        }
    }

    // Basically just removes any diacritics etc so we can add eg "Kamahi" for searching etc
    @NonNull
    public static String clean(String input) {
        String normalized = normalize(input, Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    public static void generateDatabase(iProgressCallback callback) throws IOException {
        downloadCSVFile();
        readCSV(callback);
        if (callback != null) {
            callback.finish();
        }
        populateDatabase(preferredIDs, synonyms);
        clearCache();
    }
}
