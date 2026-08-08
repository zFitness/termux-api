package com.termux.api.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.api.TermuxAPIApplication;
import com.termux.api.settings.activities.TermuxAPISettingsActivity;
import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.android.PermissionUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.theme.TermuxThemeUtils;
import com.termux.shared.theme.NightMode;
import com.termux.api.R;

public class TermuxAPIMainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "TermuxAPIMainActivity";

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.logDebug(LOG_TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_termux_api_main);

        // Set NightMode.APP_NIGHT_MODE
        TermuxThemeUtils.setAppNightMode(this);
        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        mWebView = findViewById(R.id.webview);
        setupWebView();
    }

    private void setupWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        mWebView.setBackgroundColor(Color.parseColor("#0a0e14"));

        mWebView.addJavascriptInterface(new TermuxApiInterface(this), "TermuxApi");

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Open external links (if any) in the system browser.
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    try {
                        view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception e) {
                        Logger.logError(LOG_TAG, "Failed to open url \"" + url + "\": " + e.getMessage());
                    }
                    return true;
                }
                // Let the WebView handle internal navigation between asset pages.
                return false;
            }
        });

        mWebView.loadUrl("file:///android_asset/overview.html");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Set log level for the app
        TermuxAPIApplication.setLogConfig(this, false);

        Logger.logVerbose(LOG_TAG, "onResume");

        // Refresh warning states shown on the page after returning from a permission screen.
        if (mWebView != null) {
            mWebView.evaluateJavascript("if (window.TermuxApiRefresh) window.TermuxApiRefresh();", null);
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public void openSettings() {
        ActivityUtils.startActivity(this, new Intent().setClass(this, TermuxAPISettingsActivity.class));
    }

    public void requestDisableBatteryOptimizations() {
        Logger.logDebug(LOG_TAG, "Requesting to disable battery optimizations");
        PermissionUtils.requestDisableBatteryOptimizations(this, PermissionUtils.REQUEST_DISABLE_BATTERY_OPTIMIZATIONS);
    }

    public void requestDisplayOverOtherAppsPermission() {
        Logger.logDebug(LOG_TAG, "Requesting to grant display over other apps permission");
        PermissionUtils.requestDisplayOverOtherAppsPermission(this, PermissionUtils.REQUEST_GRANT_DISPLAY_OVER_OTHER_APPS_PERMISSION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.logVerbose(LOG_TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode: " + resultCode + ", data: " + IntentUtils.getIntentString(data));

        switch (requestCode) {
            case PermissionUtils.REQUEST_DISABLE_BATTERY_OPTIMIZATIONS:
            case PermissionUtils.REQUEST_GRANT_DISPLAY_OVER_OTHER_APPS_PERMISSION:
                // The page refreshes itself in onResume.
                break;
            default:
                Logger.logError(LOG_TAG, "Unknown request code \"" + requestCode + "\" passed to onRequestPermissionsResult");
        }
    }
}
