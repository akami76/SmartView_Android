package com.kt.smartview.push;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.kt.smartview.support.log.YWMLog;
import com.kt.smartview.support.preference.SmartViewPreference;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class RestartService extends WakefulBroadcastReceiver {
	private final YWMLog logger = new YWMLog(this.getClass());
	@Override
	public void onReceive(final Context context, Intent intent) {
		logger.d("RestartService onReceive...");
		if(SmartViewPreference.isPushOn(context)){
			if(intent == null){
				intent = new Intent();
			}
			intent.setClass(context, PushService.class);
			intent.setAction(PushService.ACTION);
			startWakefulService(context.getApplicationContext(), intent);
		}
	}
}
