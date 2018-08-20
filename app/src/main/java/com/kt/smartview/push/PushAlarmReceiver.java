package com.kt.smartview.push;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.kt.smartview.support.preference.SmartViewPreference;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class PushAlarmReceiver extends WakefulBroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        if(SmartViewPreference.isPushOn(context) && SmartViewPreference.isLogin(context)){
            if(intent == null){
                intent = new Intent();
            }
            intent.setClass(context, PushService.class);
            intent.setAction(PushService.ACTION);
            startWakefulService(context.getApplicationContext(), intent);
        }
    }
}