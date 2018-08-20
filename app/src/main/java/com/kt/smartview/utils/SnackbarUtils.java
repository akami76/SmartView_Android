package com.kt.smartview.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kt.smartview.R;

/**
 * Created by louis on 2016-06-14.
 */
public class SnackbarUtils {

    public static void show(View view, String message) {
        getSnackbar(view, message).show();
    }

    public static void show(View view, int resId) {
        getSnackbar(view, resId).show();
    }

    public static void show(Context context, int resId) {
        if((context instanceof  Activity) == false) return;
        show((Activity)context, resId);
    }

    public static void show(Context context, String message) {
        if((context instanceof  Activity) == false) return;
        show((Activity)context, message);
    }

    public static void show(Activity activity, String message) {
        if(activity == null || activity.isFinishing()) return;
        ViewGroup viewGroup = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        show(viewGroup, message);
    }

    public static void show(Activity activity, int resId) {
        if(activity == null || activity.isFinishing()) return;
        ViewGroup viewGroup = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        show(viewGroup, resId);
    }

    public static void showAction(View view, int resId, int action, View.OnClickListener listener) {
        Snackbar.make(view, resId, 10000).setAction(action, listener).show();
        getSnackbar(view, resId).setAction(action, listener).show();
    }

    public static void showAction(Activity activity, int resId, int action, View.OnClickListener listener) {
        if(activity == null || activity.isFinishing()) return;
        ViewGroup viewGroup = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        showAction(viewGroup, resId, action, listener);
    }

    public static Snackbar getSnackbar(View view, String message){
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
        layout.setBackgroundResource(R.drawable.bg_gradient_teal);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            layout.setElevation(10);
        }
        return snackbar;
    }
    public static Snackbar getSnackbar(View view, int redId){
        Snackbar snackbar = Snackbar.make(view, redId, Snackbar.LENGTH_SHORT);
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
        layout.setBackgroundResource(R.drawable.bg_gradient_teal);
        TextView txtAction = (TextView) layout.findViewById(android.support.design.R.id.snackbar_action);
        txtAction.setTextColor(Color.WHITE);
        txtAction.setTypeface(null, Typeface.BOLD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            layout.setElevation(10);
        }
        return snackbar;
    }

}
