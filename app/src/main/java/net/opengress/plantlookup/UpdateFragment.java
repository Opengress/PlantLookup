package net.opengress.plantlookup;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static net.opengress.plantlookup.utils.FileUtils.getLastModifiedHeader;
import static net.opengress.plantlookup.utils.SetupUtils.generateDatabase;

import static java.text.MessageFormat.format;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.opengress.plantlookup.utils.ProgressCallback;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class UpdateFragment extends Fragment {

    SharedPreferences prefs;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);
        prefs = requireActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);

        View view = inflater.inflate(R.layout.fragment_update, container, false);

        AtomicReference<Date> local = new AtomicReference<>(updateTimestampView(view));

        view.findViewById(R.id.update_button).setOnClickListener(v -> {
            prefs.edit().putBoolean("db_setup_in_progress", true).apply();
            prefs.edit().putLong("version_warning_snoozed", 0).apply();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    Date remote = getLastModifiedHeader("https://nvs.landcareresearch.co.nz/Content/CurrentNVSNames.csv");

                    if (remote.before(local.get())) {
                        String remoteStr = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(remote);
                        alert(format("Your data is up to date! No update available since {0}.", remoteStr));
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            requireActivity().findViewById(R.id.update_progress).setVisibility(VISIBLE);
                            requireActivity().findViewById(R.id.update_button).setEnabled(false);
                        });
                        generateDatabase(new ProgressCallback(requireActivity(), R.id.update_progress));
                        requireActivity().runOnUiThread(() -> {
                            requireActivity().findViewById(R.id.update_progress).setVisibility(INVISIBLE);
                            prefs.edit().remove("db_setup_in_progress").apply();
                            local.set(updateTimestampView(view));
                            alert("The update completed successfully.");
                        });
                    }
                } catch (IOException e) {
                    Log.e("UPDATE", e.toString());
                    requireActivity().runOnUiThread(() -> {
                        requireActivity().findViewById(R.id.update_progress).setVisibility(INVISIBLE);
                        alert("Couldn't check for updates.");
                    });
                }
            });
        });
        return view;
    }

    @NonNull
    private static Date updateTimestampView(@NonNull View view) {
        Date local = DatabaseManager.getLastUpdateTime();
        String localStr = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(local);
        view.<TextView>findViewById(R.id.update_status_text).setText(format("Your database was last updated {0}. Press below to download any available updates.", localStr));
        return local;
    }

    private void alert(String message) {
        if (getActivity() == null) {
            return;
        }
        requireActivity().runOnUiThread(() -> new AlertDialog.Builder(getContext())
                .setMessage(message)
                .show());
    }
}
