package com.kt.smartview.support.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Set;

/**
 * @author jkjeon.
 * @project SmartView.
 * @date 2016-12-22.
 */
public class SettingPreference {

    public static SharedPreferences getDefaultSharedPreferences(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String getString(Context context, String name, String value) {
        return getDefaultSharedPreferences(context)
                .getString(name, value);
    }
    public static boolean setString(Context context, String name, String value) {
        return getDefaultSharedPreferences(context)
                .edit().putString(name, value).commit();
    }
    public static boolean setInt(Context context, String name, int value) {
        return getDefaultSharedPreferences(context)
                .edit().putInt(name, value).commit();
    }
    public static int getInt(Context context, String name, int value) {
        return getDefaultSharedPreferences(context)
                .getInt(name, value);
    }

    public static boolean setFloat(Context context, String name, float value) {
        return getDefaultSharedPreferences(context)
                .edit().putFloat(name, value).commit();
    }
    public static float getFloat(Context context, String name, float value) {
        return getDefaultSharedPreferences(context)
                .getFloat(name, value);
    }

    public static boolean setBoolean(Context context, String name, boolean value) {
        return getDefaultSharedPreferences(context)
                .edit().putBoolean(name, value).commit();
    }
    public static boolean getBoolean(Context context, String name, boolean value) {
        return getDefaultSharedPreferences(context)
                .getBoolean(name, value);
    }

    public static boolean setLong(Context context, String name, long value) {
        return getDefaultSharedPreferences(context)
                .edit().putLong(name, value).commit();
    }
    public static long getLong(Context context, String name, long value) {
        return getDefaultSharedPreferences(context)
                .getLong(name, value);
    }

    public static boolean setStringLong(Context context, String name, Set<String> value) {
        return getDefaultSharedPreferences(context)
                .edit().putStringSet(name, value).commit();
    }
    public static Set<String> getStringLong(Context context, String name, Set<String> value) {
        return getDefaultSharedPreferences(context)
                .getStringSet(name, value);
    }

    /*public static boolean isPushOn(Context context) {
        return getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.key_push_on), true);
    }

    public static void isPushOn(Context context, boolean pushOn) {
        getDefaultSharedPreferences(context)
                .edit().putBoolean(context.getString(R.string.key_push_on), pushOn).commit();
    }*/
}
