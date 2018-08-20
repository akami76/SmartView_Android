package com.kt.smartview.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.kt.smartview.R;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class AlertFragment extends DialogFragment {

    private DialogListener mDialogListener;
    private DialogOneButtonListener mDialogOneButtonListener;

    private static final String PARAM_ICON = "PARAM_ICON";
    private static final String PARAM_TITLE = "PARAM_TITLE";
    private static final String PARAM_MSG = "PARAM_MSG";
    private static final String PARAM_POSITIVE = "PARAM_POSITIVE";
    private static final String PARAM_NEGATIVE = "PARAM_NEGATIVE";
    private static final String PARAM_ISCANCEL = "PARAM_ISCANCEL";

    /**
     * AlertFragment
     * @param icon
     * @param title
     * @param msg
     * @param positive
     * @param negative
     * @param isCancel
     * @param listener
     * @return
     */
    public static AlertFragment newInstance(int icon, String title, String msg, String positive, String negative, boolean isCancel, DialogListener listener) {
        AlertFragment fragment = new AlertFragment();
        fragment.setListener(listener);
        Bundle args = new Bundle();
        args.putInt(PARAM_ICON, icon);
        args.putString(PARAM_TITLE, title);
        args.putString(PARAM_MSG, msg);
        args.putString(PARAM_POSITIVE, positive);
        args.putString(PARAM_NEGATIVE, negative);
        args.putBoolean(PARAM_ISCANCEL, isCancel);
        fragment.setArguments(args);
        return fragment;
    }

    public static AlertFragment newInstance(int icon, String title, String msg, String positive, boolean isCancel, DialogOneButtonListener listener) {
        AlertFragment fragment = new AlertFragment();
        fragment.setOneButtonListener(listener);
        Bundle args = new Bundle();
        args.putInt(PARAM_ICON, icon);
        args.putString(PARAM_TITLE, title);
        args.putString(PARAM_MSG, msg);
        args.putString(PARAM_POSITIVE, positive);
        args.putBoolean(PARAM_ISCANCEL, isCancel);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int icon = 0;
        String title = "";
        String msg = "";
        String positive = "";
        String negative = "";
        boolean isCancel = false;

        if (getArguments() != null) {
            icon = getArguments().getInt(PARAM_ICON);
            title = getArguments().getString(PARAM_TITLE);
            msg = getArguments().getString(PARAM_MSG);
            positive = getArguments().getString(PARAM_POSITIVE);
            negative = getArguments().getString(PARAM_NEGATIVE);
            isCancel = getArguments().getBoolean(PARAM_ISCANCEL);
        }
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.item_dialog, null);
        TextView textView = (TextView)view.findViewById(R.id.DIALOG_txt_msg);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);

        if (0 != icon) {
            builder.setIcon(icon);
        }
        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }
        if (!TextUtils.isEmpty(msg)) {
            textView.setText(msg);
        }
        builder.setPositiveButton(positive,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        if(mDialogListener != null){
                            mDialogListener.onDialogPositiveClick();
                        } else {
                            if (mDialogOneButtonListener != null) {
                                mDialogOneButtonListener.onDialogPositiveClick();
                            }
                        }
                        dismiss();
                    }
                }
        );

        if(!TextUtils.isEmpty(negative)){
            builder.setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    mDialogListener.onDialogNegativeClick();
                                    dismiss();
                                }
                            }
                    );
        }
        builder.setCancelable(isCancel);
        return builder.create();
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            FragmentTransaction ft = manager.beginTransaction();
            ft.add(this, tag);
            ft.commitAllowingStateLoss();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void setListener(DialogListener listener) {
        mDialogListener = listener;
    }
    public void setOneButtonListener(DialogOneButtonListener listener) {
        mDialogOneButtonListener = listener;
    }

    public interface DialogListener {
        void onDialogPositiveClick();

        void onDialogNegativeClick();
    }

    public interface DialogOneButtonListener {
        void onDialogPositiveClick();
    }
}
