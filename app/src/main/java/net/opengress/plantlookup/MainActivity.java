package net.opengress.plantlookup;

import static android.view.View.VISIBLE;
import static net.opengress.plantlookup.utils.SetupUtils.generateDatabase;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.snackbar.Snackbar;

import net.opengress.plantlookup.databinding.ActivityMainBinding;
import net.opengress.plantlookup.utils.ProgressCallback;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private boolean doubleBackToExitPressedOnce = false;
    SharedPreferences prefs;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String PREF_DISMISSED = "version_warning_dismissed";
    private static final String PREF_SNOOZED = "version_warning_snoozed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        showAppUpdateAlert();

        if (!DatabaseManager.getFile().exists() || prefs.getBoolean("db_setup_in_progress", false)) {
            showFirstRunScreen();
        } else {
            showMainUI();
        }
    }

    private void showActualAlert() {

        var builder = new AlertDialog.Builder(this);
        TextView messageView = new TextView(this);
        var message = getString(R.string.last_play_store_version);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            messageView.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
        } else {
            messageView.setText(Html.fromHtml(message));
        }
        messageView.setMovementMethod(LinkMovementMethod.getInstance());
        messageView.setPadding(40, 20, 40, 20); // Optional padding for better UI
        builder.setTitle("Important Update Information")
                .setView(messageView)
                .setPositiveButton("Visit website", (dialog, which) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://chris-nz.com/plantlookup/"));
                    startActivity(browserIntent);
                    dialog.dismiss();
                })
                .setNegativeButton("Minimise notice", (dialog, which) -> {
                    prefs.edit().putBoolean(PREF_DISMISSED, true).apply();
                    showUpdateSnackbar();
                    dialog.dismiss();
                })
                .setNeutralButton("Remind me", (dialog, which) -> {
                    prefs.edit().putLong(PREF_SNOOZED, System.currentTimeMillis() + (72 * 60 * 60 * 1000)).apply();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void showUpdateSnackbar() {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView,
                        "This is the final Play Store version.", Snackbar.LENGTH_INDEFINITE)
                .setAction("More Info", v -> {
                    // Reopen the dialog
                    prefs.edit().putBoolean(PREF_DISMISSED, false).apply();
                    showAppUpdateAlert();
                });

        snackbar.show();
    }

    private void showAppUpdateAlert() {
        boolean dismissed = prefs.getBoolean(PREF_DISMISSED, false);
        long lastSeen = prefs.getLong(PREF_SNOOZED, 0);
        long currentTime = System.currentTimeMillis();

        Log.d("UpdateAlert", "dismissed: " + dismissed + ", lastSeen: " + lastSeen + ", currentTime: " + currentTime);
        Log.d("UpdateAlert", "dismissed: " + dismissed + ", lastSeen: " + lastSeen + ", currentTime: " + currentTime);
        Log.d("UpdateAlert", "dismissed: " + dismissed + ", lastSeen: " + lastSeen + ", currentTime: " + currentTime);
        Log.d("UpdateAlert", "dismissed: " + dismissed + ", lastSeen: " + lastSeen + ", currentTime: " + currentTime);
        Log.d("UpdateAlert", "dismissed: " + dismissed + ", lastSeen: " + lastSeen + ", currentTime: " + currentTime);
        Log.d("UpdateAlert", "dismissed: " + dismissed + ", lastSeen: " + lastSeen + ", currentTime: " + currentTime);
        Log.d("UpdateAlert", "dismissed: " + dismissed + ", lastSeen: " + lastSeen + ", currentTime: " + currentTime);
        Log.d("UpdateAlert", "dismissed: " + dismissed + ", lastSeen: " + lastSeen + ", currentTime: " + currentTime);

        if (dismissed) {
            Log.d("UpdateAlert", "Showing snackbar");
            showUpdateSnackbar();
        } else if (currentTime - lastSeen > 72 * 60 * 60 * 1000) {
            showActualAlert();
        } else {
            Log.d("UpdateAlert", "Conditions not met for snackbar or alert");
        }
    }

    private void showFirstRunScreen() {
        prefs.edit().putBoolean("db_setup_in_progress", true).apply();
        setContentView(R.layout.fragment_setup);

        findViewById(R.id.setup_button).setOnClickListener(v -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    runOnUiThread(() -> {
                        findViewById(R.id.setup_progress).setVisibility(VISIBLE);
                        findViewById(R.id.setup_button).setEnabled(false);
                    });
                    generateDatabase(new ProgressCallback(MainActivity.this, R.id.setup_progress));
                    runOnUiThread(() -> {
                        prefs.edit().remove("db_setup_in_progress").apply();
                        showMainUI();
                    }); // Proceed once done
                } catch (IOException e) {
                    Log.e("UPDATE", e.toString());
                    runOnUiThread(() -> alert("Database setup failed."));
                }
            });
        });
    }

    private void showMainUI() {
        if (isFinishing()) {
            Log.e("MAIN", "Impossibly, we are quitting");
            return;
        }
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.action_update) {
            Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                    .navigate(R.id.UpdateFragment);

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // Guard against accidental quit. Double-pressing back works. Alternatively there's a prompt.
    @Override
    public void onBackPressed() {

        ProgressBar bar = findViewById(R.id.setup_progress);
        if (bar != null && bar.getVisibility() == VISIBLE && bar.isIndeterminate()) {
            alert("Quitting during setup operations could have horrible consequences.");
            return;
        }

        try {

            // if we aren't on the first screen, then NO-OP
            NavDestination currentDestination = Navigation.findNavController(this, R.id.nav_host_fragment_content_main).getCurrentDestination();
            if (currentDestination == null || currentDestination.getId() != R.id.SearchFragment) {
                super.onBackPressed();
                return;
            }

            // if we are on the first screen and we have text in the text box, just clear that text
            SearchView search = requireNonNull(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main)).requireView().findViewById(R.id.plant_search);
            if (search.getQuery().length() > 0) {
                search.setQuery("", false);
                return;
            }
        } catch (IllegalArgumentException e) {
            Log.w("MAIN", "User pressed back at a funny time");
        }

        // double-back will quit in most situations
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed(); // Exit the app
            return;
        }
        this.doubleBackToExitPressedOnce = true;

        // if we're on the main screen, with empty search box, and this is our first time pressing back, prompt the user
        var snackbar = Snackbar.make(findViewById(android.R.id.content), "Really exit?", Snackbar.LENGTH_SHORT).setAction("YES", v -> super.onBackPressed());
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                if (!isFinishing()) {
                    showUpdateSnackbar();
                }
            }
        });
        snackbar.show();
        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000); // Reset the double back press flag after 2 seconds
    }

    private void alert(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }

    private void showAboutDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_about, null);
        view.<TextView>findViewById(R.id.titleTextView).setText(format("%s version %s", view.<TextView>findViewById(R.id.titleTextView).getText(), Application.getVersion()));
        view.<TextView>findViewById(R.id.yearLicenseTextView).setText(Html.fromHtml(getString(R.string.year_license_info)));
        view.<TextView>findViewById(R.id.yearLicenseTextView).setMovementMethod(LinkMovementMethod.getInstance());
        view.<TextView>findViewById(R.id.websiteTextView).setText(Html.fromHtml(getString(R.string.about_website)));
        view.<TextView>findViewById(R.id.websiteTextView).setMovementMethod(LinkMovementMethod.getInstance());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view).setPositiveButton("OK", (dialog, id) -> {
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}