package com.kt.smartview.support.preference;

import android.content.Context;
import android.text.TextUtils;

import com.kt.smartview.R;
import com.kt.smartview.common.Constants;
import com.kt.smartview.utils.CommonUtil;

/**
 * @author jkjeon.
 * @project DomaLife.
 * @date 2016-06-02.
 */
public class SmartViewPreference {

    public static final String PREF_USER_ID = "PREF_USER_ID"; // 사용자 아이디
    public static final String PREF_USER_NAME = "PREF_USER_NAME"; // 사용자 이름
    public static final String PREF_IS_LOGIN = "PREF_IS_LOGIN"; // 로그인 여부
    public static final String PREF_SETTING_TEXT = "PREF_SETTING_TEXT";
    public static final String PREF_SAVE_ID = "PREF_SAVE_ID";

    public static void logout(Context context) {
        if(context == null) return;
//        setUserId(context, null);
        setUserName(context, null);
        setLogin(context,  false);
        CommonUtil.getGlobalApplication(context).stopPushManager();
    }

    public static boolean isLogin(Context context) {
        if(context == null) return false;
        return context.getSharedPreferences(Constants.CONFIG_TAG, Context.MODE_PRIVATE).getBoolean(PREF_IS_LOGIN, false);
    }

    public static void setLogin(Context context, boolean isLogin) {
        if(context == null) return;
        context.getSharedPreferences(Constants.CONFIG_TAG, Context.MODE_PRIVATE).edit().putBoolean(PREF_IS_LOGIN, isLogin).commit();
    }

    public static String getUserId(Context context) {
        if(context == null) return null;
        return context.getSharedPreferences(Constants.CONFIG_TAG, Context.MODE_PRIVATE).getString(PREF_USER_ID, null);
    }

    public static void setUserId(Context context, String userId) {
        if(context == null) return;
        context.getSharedPreferences(Constants.CONFIG_TAG, Context.MODE_PRIVATE).edit().putString(PREF_USER_ID, userId).commit();
    }

    public static String getUserName(Context context) {
        if(context == null) return null;
        return context.getSharedPreferences(Constants.CONFIG_TAG, Context.MODE_PRIVATE).getString(PREF_USER_NAME, null);
    }

    public static void setUserName(Context context, String userName) {
        if(context == null) return;
        context.getSharedPreferences(Constants.CONFIG_TAG, Context.MODE_PRIVATE).edit().putString(PREF_USER_NAME, userName).commit();
    }

    public static boolean isPushOn(Context context) {
        /*
         * team39 isPushOn --> 로그인 안해도 됨.
         */
        return (SettingPreference.getBoolean(context, context.getString(R.string.key_push_on), true) && isLogin(context) && (TextUtils.isEmpty(getSettingText(context, null)) == false));
    }

    public static String getSettingText(Context context, String defaultValue) {
        if(context == null) return defaultValue;
        return context.getSharedPreferences(Constants.CONFIG_TAG, Context.MODE_PRIVATE).getString(PREF_SETTING_TEXT, defaultValue);
    }

    public static void setSettingText(Context context, String text) {
        if(context == null) return;
        context.getSharedPreferences(Constants.CONFIG_TAG, Context.MODE_PRIVATE).edit().putString(PREF_SETTING_TEXT, text).commit();
    }

    public static boolean isSaveId(Context context) {
        if(context == null) return false;
        return context.getSharedPreferences(Constants.CONFIG_TAG, Context.MODE_PRIVATE).getBoolean(PREF_SAVE_ID, true);
    }

    public static void setSaveId(Context context, boolean isSave) {
        if(context == null) return;
        context.getSharedPreferences(Constants.CONFIG_TAG, Context.MODE_PRIVATE).edit().putBoolean(PREF_SAVE_ID, isSave).commit();
    }
}
