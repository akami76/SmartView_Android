package com.kt.smartview.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

import com.kt.smartview.R;
import com.kt.smartview.ui.fragment.AlertFragment;

public class DialogUtils {

    /**
     * 기본 Dialog
     * int형
     *
     * @param context
     * @param fm
     * @param icon
     * @param title
     * @param msg
     * @param positive
     * @param negative
     * @param listener
     */
    public static void showDialog(Context context, FragmentManager fm, int icon, int title, int msg, int positive, int negative, AlertFragment.DialogListener listener) {
        DialogFragment dialog = AlertFragment.newInstance(icon, context.getString(title), context.getString(msg), context.getString(positive), context.getString(negative), false, listener);
        dialog.show(fm, "AlertFragment");
    }

    /**
     * 기본 Dialog
     * String형
     *
     * @param context
     * @param fm
     * @param icon
     * @param title
     * @param msg
     * @param positive
     * @param negative
     * @param listener
     */
    public static void showDialog(Context context, FragmentManager fm, int icon, String title, String msg, String positive, String negative, AlertFragment.DialogListener listener) {
        DialogFragment dialog = AlertFragment.newInstance(icon, title, msg, positive, negative, false, listener);
        dialog.show(fm, "AlertFragment");
    }

    /**
     * 메세지, 버튼 한 개
     * String형
     *
     * @param context
     * @param fm
     * @param msg
     * @param listener
     */
    public static void showDialog(Context context, FragmentManager fm, String msg, AlertFragment.DialogOneButtonListener listener) {
        DialogFragment dialog = AlertFragment.newInstance(0, "", msg, context.getString(R.string.confirm), true, listener);
        dialog.show(fm, "AlertFragment");
    }

    /**
     * 메세지, 버튼 한 개, Cancelable
     * String형
     *
     * @param context
     * @param fm
     * @param msg
     * @param listener
     */
    public static void showDialog(Context context, FragmentManager fm, String msg, boolean isCancel, AlertFragment.DialogOneButtonListener listener) {
        DialogFragment dialog = AlertFragment.newInstance(0, "", msg, context.getString(R.string.confirm), isCancel, listener);
        dialog.setCancelable(isCancel);
        dialog.show(fm, "AlertFragment");
    }

    /**
     * 메세지, 버튼 한 개
     * int형
     *
     * @param context
     * @param fm
     * @param msg
     * @param listener
     */
    public static void showDialog(Context context, FragmentManager fm, int msg, AlertFragment.DialogOneButtonListener listener) {
        DialogFragment dialog = AlertFragment.newInstance(0, "", context.getString(msg), context.getString(R.string.confirm), true, listener);
        dialog.show(fm, "AlertFragment");
    }

    /**
     * 메세지, 버튼 한 개
     * String형
     *
     * @param context
     * @param fm
     * @param msg
     * @param listener
     */
    public static void showDialog(Context context, FragmentManager fm, String title, String msg, AlertFragment.DialogOneButtonListener listener) {
        DialogFragment dialog = AlertFragment.newInstance(0, title, msg, context.getString(R.string.confirm), true, listener);
        dialog.show(fm, "AlertFragment");
    }

    /**
     * 메세지, 버튼 한 개, 버튼입력
     * int형
     *
     * @param context
     * @param fm
     * @param msg
     * @param listener
     */
    public static void showDialog(Context context, FragmentManager fm, int msg, int positive, AlertFragment.DialogOneButtonListener listener) {
        DialogFragment dialog = AlertFragment.newInstance(0, "", context.getString(msg), context.getString(positive), true, listener);
        dialog.show(fm, "AlertFragment");
    }

    /**
     * 메세지, 버튼 한 개, 버튼입력
     * String형
     *
     * @param context
     * @param fm
     * @param msg
     * @param listener
     */
    public static void showDialog(Context context, FragmentManager fm, String msg, int positive, AlertFragment.DialogOneButtonListener listener) {
        DialogFragment dialog = AlertFragment.newInstance(0, "", msg, context.getString(positive), true, listener);
        dialog.show(fm, "AlertFragment");
    }

    /**
     * 메세지, 버튼 두 개
     * String형
     *
     * @param context
     * @param fm
     * @param msg
     * @param listener
     */
    public static void showDialog(Context context, FragmentManager fm, String msg, int positive, AlertFragment.DialogListener listener) {
        DialogFragment dialog = AlertFragment.newInstance(0, "", msg, context.getString(positive), context.getString(R.string.cancel), false, listener);
        dialog.show(fm, "AlertFragment");
    }

    /**
     * 메세지, 버튼 두 개
     * int형
     *
     * @param context
     * @param fm
     * @param msg
     * @param listener
     */
    public static void showDialog(Context context, FragmentManager fm, int msg, int positive, AlertFragment.DialogListener listener) {
        DialogFragment dialog = AlertFragment.newInstance(0, "", context.getString(msg), context.getString(positive), context.getString(R.string.cancel), false, listener);
        dialog.show(fm, "AlertFragment");
    }

    /**
     * 타이틀, 메세지, 버튼 두 개
     * int형
     *
     * @param context
     * @param fm
     * @param msg
     * @param listener
     */
    public static void showDialog(Context context, FragmentManager fm, String title, String msg, int positive, AlertFragment.DialogListener listener) {
        DialogFragment dialog = AlertFragment.newInstance(0, title, msg, context.getString(positive), context.getString(R.string.cancel), false, listener);
        dialog.show(fm, "AlertFragment");
    }

    /**
     * 타이틀, 메세지, 버튼 두 개
     * int형
     *
     * @param context
     * @param fm
     * @param msg
     * @param listener
     */
    public static void showDialog(Context context, FragmentManager fm, int title, int msg, int positive, AlertFragment.DialogListener listener) {
        DialogFragment dialog = AlertFragment.newInstance(0, context.getString(title), context.getString(msg), context.getString(positive), context.getString(R.string.cancel), false, listener);
        dialog.show(fm, "AlertFragment");
    }

    /**
     * 타이틀, 메세지, 버튼 두 개
     * int형
     *
     * @param context
     * @param fm
     * @param msg
     * @param listener
     */
    public static void showDialog(Context context, FragmentManager fm, int title, int msg, int positive, boolean isCancel, AlertFragment.DialogListener listener) {
        DialogFragment dialog = AlertFragment.newInstance(0, context.getString(title), context.getString(msg), context.getString(positive), context.getString(R.string.cancel), isCancel, listener);
        dialog.setCancelable(isCancel);
        dialog.show(fm, "AlertFragment");
    }


    public static AlertDialog showDialog(Context context, int message, int positive, final DialogListener lisetener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setPositiveButton(positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        lisetener.onDialogPositiveClick();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        lisetener.onDialogNegativeClick();
                    }
                });
        return builder.create();
    }

    public static AlertDialog showDialog(Context context, int message, int positive, final DialogOneButtonListener lisetener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setPositiveButton(positive, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                lisetener.onDialogPositiveClick();
            }
        });
        return builder.create();
    }

    public interface DialogListener {
        void onDialogPositiveClick();

        void onDialogNegativeClick();
    }

    public interface DialogOneButtonListener {
        void onDialogPositiveClick();
    }
}
