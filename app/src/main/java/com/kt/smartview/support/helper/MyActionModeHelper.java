package com.kt.smartview.support.helper;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.helpers.ActionModeHelper;

public class MyActionModeHelper extends ActionModeHelper {

    public MyActionModeHelper(FlexibleAdapter adapter, int cabMenu, ActionMode.Callback callback) {
        super(adapter, cabMenu, callback);
    }

    public ActionMode startActionMode(AppCompatActivity activity){
        if (mActionMode == null) {
            mActionMode = activity.startSupportActionMode(this);
        }
        return mActionMode;
    }
}
