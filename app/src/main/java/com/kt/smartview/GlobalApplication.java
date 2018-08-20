package com.kt.smartview;

import android.content.Context;
import android.content.Intent;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.kt.smartview.support.log.YWMLog;
import com.kt.smartview.db.DomaDBAdapter;
import com.kt.smartview.db.DomaDBHandler;
import com.kt.smartview.db.DomaDBHelper;
import com.kt.smartview.network.HttpService;
import com.kt.smartview.push.PushService;
import com.kt.smartview.support.notification.NotificationCenter;
import com.kt.smartview.support.preference.SmartViewPreference;

import java.util.ArrayList;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class GlobalApplication extends MultiDexApplication {

    private final YWMLog logger = new YWMLog(GlobalApplication.class);
    private static GlobalApplication mInstance;
    private static Context mContext;
    private DomaDBAdapter mWritableDBAdapter = null;
    private DomaDBAdapter mReadableDBAdapter = null;
    public static final int DATABASE_VERSION = 1;
    public static GlobalApplication getInstance() {
        return mInstance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mContext = getApplicationContext();
        init();
    }

    private void init() {
        HttpService.init();

        boolean isExistDB = DomaDBHelper.checkDatabase();
        mWritableDBAdapter = new DomaDBAdapter(getApplicationContext(), false);
        mReadableDBAdapter = new DomaDBAdapter(getApplicationContext(), true);
        DomaDBHelper dbHelper = DomaDBHelper.getInstance(getApplicationContext());
        DomaDBHandler dbHandler = new DomaDBHandler(getApplicationContext());
        if(isExistDB){
            int version = dbHandler.getCurrentDBVersion();
            if(version == -1 || DATABASE_VERSION > version){
                dbHandler.updateDBVersion(DATABASE_VERSION);
            }
        }else{
            dbHelper.createDatabase(true);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        logger.d("GlobalApplication onLowMemory------------------");
    }

    @Override
    public void onTerminate() {
        NotificationCenter notificationCenter = new NotificationCenter(getApplicationContext());
        notificationCenter.removeAll();
        super.onTerminate();
    }

    public static Context getContext() {
        return mContext;
    }

    public DomaDBAdapter getWritableDBAdapter() {
        return mWritableDBAdapter;
    }

    public DomaDBAdapter getReadableDBAdapter() {
        return mReadableDBAdapter;
    }

    public void startPushManager(){
        if(SmartViewPreference.isPushOn(getApplicationContext())){
            Log.i("TEST", "GlobalApplication.startPushManager-->");
            Intent intent = new Intent(getApplicationContext(), PushService.class);
            intent.setAction(PushService.ACTION);
            startService(intent);
        }else{
            stopPushManager();
        }
    }

    public void stopPushManager(){
        Intent intent = new Intent(getApplicationContext(), PushService.class);
        intent.setAction(PushService.ACTION);
        stopService(intent);
    }

    public void refreshSubscribe(ArrayList<String> list){
        Intent intent = new Intent(getApplicationContext(), PushService.class);
        intent.setAction(PushService.ACTION_TOPIC_RELOAD);
        intent.putExtra(PushService.EXTRA_TOPIC_LIST, list);
        startService(intent);
    }
    public void onSubscribeDeleted(String roomId){
        Intent intent = new Intent(getApplicationContext(), PushService.class);
        intent.setAction(PushService.ACTION_TOPIC_DELETED);
        intent.putExtra(PushService.EXTRA_TOPIC_NAME, roomId);
        startService(intent);
    }
    public void onSubscribeInserted(String roomId){
        Intent intent = new Intent(getApplicationContext(), PushService.class);
        intent.setAction(PushService.ACTION_TOPIC_ADDED);
        intent.putExtra(PushService.EXTRA_TOPIC_NAME, roomId);
        startService(intent);
    }
};
