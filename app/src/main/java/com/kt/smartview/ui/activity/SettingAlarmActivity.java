package com.kt.smartview.ui.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.kt.smartview.R;
import com.kt.smartview.ui.fragment.SettingAlarmFragment;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class SettingAlarmActivity extends BaseCompatActivity {

    private OnBackkeyPressedListener mOnBackkeyPressedListener;

    public interface OnBackkeyPressedListener {
        public void onBackkeyPressed();
    }

    /***********************************************************************************************
     * LIFE CYCLE
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_alarm);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setTitle(getString(R.string.settings));

        SettingAlarmFragment fragment = SettingAlarmFragment.getInstance();
        getFragmentManager().beginTransaction()
                .add(R.id.container, fragment).commit();
    }

    @Override
    public void onBackPressed() {
        if (mOnBackkeyPressedListener != null) {
            mOnBackkeyPressedListener.onBackkeyPressed();
        }
        super.onBackPressed();
    }


    /***********************************************************************************************
     * OVERRIDE
     */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /***********************************************************************************************
     * METHOD
     */
    public void setOnBackkeyPressedListener(OnBackkeyPressedListener listener) {
        mOnBackkeyPressedListener = listener;
    }

    @Override
    public void startActivityAnimation() {
        overridePendingTransition(R.anim.start_enter, R.anim.start_exit);
    }

    @Override
    public void finishActivityAnimation() {
        overridePendingTransition(R.anim.end_enter, R.anim.end_exit);
    }
}