package com.kt.smartview.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spanned;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.kt.smartview.GlobalApplication;
import com.kt.smartview.R;
import com.kt.smartview.support.log.YWMLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class CommonUtil {

    private static final YWMLog logger = new YWMLog(CommonUtil.class);

    public static GlobalApplication getGlobalApplication(Context context) {
        return ((GlobalApplication) context.getApplicationContext());
    }

    public static String getAndroidId(Context context) {
        String deviceId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return deviceId;
    }

    /**
     * 현재 네트워크 상태가 원할한지 여부를 알아내는 함수
     *
     * @param context
     * @return boolean
     * @Method Name  : isNetworkAvailable
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info == null) {
            return false;
        }
        return info.isConnected();
    }

    /**
     * Toast를 여는 함수
     *
     * @param activity
     * @param context
     * @param msg
     * @Method Name  : toast
     */
    public static void toast(final Activity activity, final Context context, final String msg, final int duration) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Toast.makeText(context, msg, duration).show();
            }
        });
    }

    public static void toast(final Context context, final String msg, final int duration) {
        Toast.makeText(context, msg, duration).show();
    }

    public static void toast(final Context context, final int msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @TargetApi(11)
    public static void copyToClipboard(Context context, String text, String toastMessage) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("SocioStation Cliped Data", text);
            clipboard.setPrimaryClip(clip);
        }
        if (toastMessage != null) {
            SnackbarUtils.show(context, toastMessage);
        }
    }

    public static void applyUnReadAlarmCountToBadge(Context context, int unReadAlarmCount){
        Intent badgeIntent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
        badgeIntent.putExtra("badge_count", unReadAlarmCount);
        badgeIntent.putExtra("badge_count_package_name", context.getPackageName());
        badgeIntent.putExtra("badge_count_class_name", CommonUtil.getLauncherClassName (context));
        context.sendBroadcast(badgeIntent);
    }

    public static String getLauncherClassName (Context context) {
        PackageManager pm = context.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            String pkgName = resolveInfo.activityInfo.applicationInfo.packageName;
            if (pkgName.equalsIgnoreCase(context.getPackageName())) {
                String className = resolveInfo.activityInfo.name;
                return className;
            }
        }
        return null;
    }

    public static String getDate(String dateformat) {
        TimeZone tz = TimeZone.getDefault();
        SimpleDateFormat formatter = new SimpleDateFormat(dateformat);
        Calendar cal = Calendar.getInstance(tz);
        Date currentTime_1 = cal.getTime();
        String dateString = formatter.format(currentTime_1);
        return dateString;
    }

    public static void share(Context context, String message) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.setType("text/plain");
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)));
    }

    public static byte[] getBytesFromFile(Context context, String fileName) {
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try{
            in = context.getAssets().open(fileName);
            out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (true) {
                int r = in.read(buffer);
                if (r == -1) break;
                out.write(buffer, 0, r);
            }

            byte[] ret = out.toByteArray();
            return ret;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }finally {
            try {
                if(in != null){
                    in.close();
                }
                if(out != null){
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 숫자인지의 여부를 확인한다.
     *
     * @param str
     *            확인할 String
     * @return double 여부(true : 성공, false : 실패)
     */
    public static boolean isNumber(String str) {
        char[] chars = str.toCharArray();
        boolean isNumber = false;
        boolean dotInserted = false;

        if ("".equals(str)) {
            return false;
        }

        // if the last char in the string is a dot, it isn't a number!
        if (str.charAt(str.length() - 1) == '.') {
            return false;
        }

        for (int i = 0; i < chars.length; i++) {
            // a dot inside the string can be a number as well
            // although it can only occur once.
            if (chars[i] == 46) {
                if (!dotInserted) {
                    dotInserted = true;
                } else {
                    return false;
                }
                // 48 is ASCII for 0, 57 for 9
            } else if (chars[i] < 48 || chars[i] > 57) {
                return false;
            } else {
                isNumber = true;
            }
        }

        return isNumber;
    }

    public static Spanned getHtmlString(String source){
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(source ,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(source);
        }
        return result;
    }

    public static void hideKeyboard(Context context, TextView textView){
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
    }

    public static String getMacAddress(Context context){
        TelephonyManager tpm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getMacAddress();
    }

    public static String getMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {

                String macStr = (nif.getHardwareAddress() == null) ? null : new String(nif.getHardwareAddress());
                logger.d(String.format("NIC [%8s] - %s", nif.getName(), macStr));

                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }
}
