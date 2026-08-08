package com.termux.api.activities;

import android.content.Context;
import android.webkit.JavascriptInterface;

import com.termux.shared.android.PermissionUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * JavaScript interface exposed to the WebView used by {@link TermuxAPIMainActivity}.
 *
 * <p>Provides the API catalog (loaded from the bundled {@code api_catalog.json} asset) and
 * app metadata to the HTML pages rendered in the WebView.</p>
 */
public class TermuxApiInterface {

    private static final String LOG_TAG = "TermuxApiInterface";

    private final Context mContext;

    private String mApisJson;

    public TermuxApiInterface(Context context) {
        mContext = context;
    }

    /**
     * Returns the bundled API catalog as a JSON array string.
     */
    @JavascriptInterface
    public String getApis() {
        if (mApisJson == null) {
            mApisJson = readAsset("api_catalog.json");
        }
        return mApisJson;
    }

    /**
     * Returns the application display name and version (e.g. "Termux:API 0.53.0").
     */
    @JavascriptInterface
    public String getVersion() {
        String versionName;
        try {
            versionName = mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0).versionName;
        } catch (Exception e) {
            versionName = "";
        }
        return TermuxConstants.TERMUX_API_APP_NAME + " " + versionName;
    }

    /**
     * Opens the app settings activity.
     */
    @JavascriptInterface
    public void openSettings() {
        if (mContext instanceof TermuxAPIMainActivity) {
            ((TermuxAPIMainActivity) mContext).openSettings();
        }
    }

    /**
     * Returns {@code true} if battery optimizations are disabled for this app.
     */
    @JavascriptInterface
    public boolean isBatteryOptimizationDisabled() {
        return PermissionUtils.checkIfBatteryOptimizationsDisabled(mContext);
    }

    /**
     * Requests the user to disable battery optimizations for this app.
     */
    @JavascriptInterface
    public void requestDisableBatteryOptimizations() {
        if (mContext instanceof TermuxAPIMainActivity) {
            ((TermuxAPIMainActivity) mContext).requestDisableBatteryOptimizations();
        }
    }

    /**
     * Returns {@code true} if the display over other apps permission is granted.
     */
    @JavascriptInterface
    public boolean isDisplayOverOtherAppsPermissionGranted() {
        return PermissionUtils.checkDisplayOverOtherAppsPermission(mContext);
    }

    /**
     * Requests the user to grant the display over other apps permission.
     */
    @JavascriptInterface
    public void requestGrantDisplayOverOtherAppsPermission() {
        if (mContext instanceof TermuxAPIMainActivity) {
            ((TermuxAPIMainActivity) mContext).requestDisplayOverOtherAppsPermission();
        }
    }

    private String readAsset(String name) {
        InputStream is = null;
        try {
            is = mContext.getAssets().open(name);
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            Logger.logError(LOG_TAG, "Failed to read asset \"" + name + "\": " + e.getMessage());
            return "[]";
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
