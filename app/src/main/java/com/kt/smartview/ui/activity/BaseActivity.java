package com.kt.smartview.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public abstract class BaseActivity extends FragmentActivity {
    public abstract void startActivityAnimation();
    public abstract void finishActivityAnimation();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivityAnimation();
    }

    @Override
    public void finish() {
        super.finish();
        finishActivityAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public Context getContext(){
        return this;
    }

    public Activity getActivity(){
        return this;
    }

}
