package net.opengress.plantlookup.utils;

import static java.util.Objects.*;

import android.util.Log;

import androidx.annotation.NonNull;

import net.opengress.plantlookup.Application;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FileUtils {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .addNetworkInterceptor(chain ->
                    chain.proceed(
                            chain.request()
                                    .newBuilder()
                                    .header("User-Agent", "Plant Lookup " + Application.getVersion() + " - https://chris-nz.com/plantlookup/")
                                    .build()
                    )
            ).build();

    @NonNull
    public static String readSync(String filePath) throws IOException {
        File file = new File(filePath);
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
             BufferedReader reader = new BufferedReader(isr)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        }
    }

    @NonNull
    public static Date getLastModifiedHeader(String url) throws IOException {
        Request request = new Request.Builder().url(url).head().build();
        try (Response response = client.newCall(request).execute()) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));  // Set to GMT to match input
            return requireNonNull(sdf.parse(requireNonNull(response.header("Last-Modified"))));
        } catch (Throwable e) {
            return new Date(0);
        }
    }

    @NonNull
    public static String downloadSync(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            return new String(response.body().bytes(), "UTF-8");
        }
    }

    public static void saveToFile(File file, String data) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            writer.write(data);
        }
    }


    public static void copy(String sourcePath, String destPath) throws IOException {
        saveToFile(new File(destPath), readSync(sourcePath));
    }

    public static void clearCache() {
        try {
            File cacheDir = Application.getInstance().getCacheDir();
            deleteDir(cacheDir);
            Log.d("CACHE", "Cache cleared.");
        } catch (Exception e) {
            Log.e("CACHE", "Failed to clear cache: " + e.getMessage());
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) return false;
                }
            }
        }
        return dir.delete();
    }

}
