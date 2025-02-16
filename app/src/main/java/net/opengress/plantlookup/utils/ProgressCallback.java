package net.opengress.plantlookup.utils;

import android.app.Activity;
import android.widget.ProgressBar;

import net.opengress.plantlookup.R;

public class ProgressCallback implements SetupUtils.iProgressCallback {
    private final Activity activity;
    private final int id;

    public ProgressCallback(Activity activity, int widget) {
        this.activity = activity;
        id = widget;
    }

    @Override
    public void onProgressUpdate(int current) {
        activity.runOnUiThread(() -> activity.<ProgressBar>findViewById(id).setProgress(current));
    }

    @Override
    public void init(int totalRecords) {
        activity.runOnUiThread(() -> {
            activity.<ProgressBar>findViewById(id).setMax(totalRecords);
            activity.<ProgressBar>findViewById(id).setIndeterminate(false);
        });
    }

    @Override
    public void finish() {
        activity.runOnUiThread(() -> activity.<ProgressBar>findViewById(id).setIndeterminate(true));
    }
}
