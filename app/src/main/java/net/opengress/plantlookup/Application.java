package net.opengress.plantlookup;

import static android.content.pm.PackageManager.GET_ACTIVITIES;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class Application extends android.app.Application {
    private static Application instance;

    public static Application getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static String getVersion() {

        String name = "[unknown]";
        try {
            PackageInfo info = instance.getPackageManager().getPackageInfo(instance.getPackageName(), GET_ACTIVITIES);
            name = info.versionName;
        } catch (Throwable ignored) {
        }

        return name;
    }
}
