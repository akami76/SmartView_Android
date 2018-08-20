package com.kt.smartview.utils;

import android.app.Activity;
import android.widget.Toast;

import com.kt.smartview.R;

public class BackPressCloseHandler {

    private static final int BACKKEY_INTERVAL = 2000;

    private long backKeyPressedTime = 0;
    private Toast toast;

    private Activity activity;

    public BackPressCloseHandler(Activity context) {
        this.activity = context;
    }

    public void onBackPressed() {
        if (System.currentTimeMillis() > backKeyPressedTime + BACKKEY_INTERVAL) {
            backKeyPressedTime = System.currentTimeMillis();
            showGuide();
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + BACKKEY_INTERVAL) {
            activity.finish();
            toast.cancel();
        }
    }

    public void showGuide() {
        toast = Toast.makeText(activity, R.string.backkey_finish_message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
