/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package com.kt.smartview.push.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.kt.smartview.support.log.YWMLog;
import com.kt.smartview.support.preference.SmartViewPreference;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

class AlarmPingSender implements MqttPingSender {
	private final YWMLog logger = new YWMLog(AlarmPingSender.class);
	static final String TAG = "AlarmPingSender";
	private Intent registIntent;
	private ClientComms comms;
	private MqttService service;
	private BroadcastReceiver alarmReceiver;
	private AlarmPingSender that;
	private PendingIntent pendingIntent;
	private volatile boolean hasStarted = false;

	public AlarmPingSender(MqttService service) {
		if (service == null) {
			throw new IllegalArgumentException("Neither service nor client can be null.");
		}
		this.service = service;
		that = this;
	}

	@Override
	public void init(ClientComms comms) {
		this.comms = comms;
		this.alarmReceiver = new AlarmReceiver();
	}

	@Override
	public void start() {
		if(SmartViewPreference.isPushOn(service.getApplicationContext()) == false){
			return;
		}
		String action = MqttServiceConstants.PING_SENDER + comms.getClient().getClientId();
		logger.d("Register alarmreceiver to MqttService" + action);
		removeReceiver();
		registIntent = service.registerReceiver(alarmReceiver, new IntentFilter(action));

		pendingIntent = PendingIntent.getBroadcast(service, 0, new Intent(action), PendingIntent.FLAG_UPDATE_CURRENT);

		schedule(comms.getKeepAlive());
		hasStarted = true;
	}

	@Override
	public void stop() {

		// 알람 취소하기
		try {
			AlarmManager alarmManager = (AlarmManager) service.getSystemService(Service.ALARM_SERVICE);
			alarmManager.cancel(pendingIntent);
		} catch (Exception e) {
			logger.e(e.toString());
		}

		logger.d("Unregister alarmreceiver to MqttService" + comms.getClient().getClientId());
		if (hasStarted) {
			hasStarted = false;
			removeReceiver();
		}
	}

	@Override
	public void schedule(long delayInMilliseconds) {

		long nextAlarmInMilliseconds = System.currentTimeMillis() + delayInMilliseconds;
		logger.d("Schedule next alarm at " + nextAlarmInMilliseconds);

		try {
			AlarmManager alarmManager = (AlarmManager) service.getSystemService(Service.ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds, pendingIntent);
		} catch (Exception e) {
			logger.e(e.toString());
		}
	}

	/*
	 * MQTT broker로 ping 패킷을 보내는 클래스
	 */
	class AlarmReceiver extends BroadcastReceiver {
		private WakeLock wakelock;
		private String wakeLockTag = MqttServiceConstants.PING_WAKELOCK + that.comms.getClient().getClientId();

		@Override
		public void onReceive(Context context, Intent intent) {
			if(SmartViewPreference.isPushOn(service.getApplicationContext()) == false){
				return;
			}
			int count = intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, -1);
			logger.d("Ping " + count + " times.");

			logger.d("Check time :" + System.currentTimeMillis());
			IMqttToken token = comms.checkForActivity();

			// 보낼 ping이 없다면
			if (token == null) {
				return;
			}

			if (wakelock == null) {
				PowerManager pm = (PowerManager) service.getSystemService(Service.POWER_SERVICE);
				wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
			}
			wakelock.acquire();
			token.setActionCallback(new IMqttActionListener() {

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					logger.d("Success. Release lock(" + wakeLockTag + "):" + System.currentTimeMillis());
					if (wakelock != null && wakelock.isHeld()) {
						wakelock.release();
					}
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					logger.d("Failure. Release lock(" + wakeLockTag + "):" + System.currentTimeMillis());
					// Wakelock 해제
					if (wakelock != null && wakelock.isHeld()) {
						wakelock.release();
					}
				}
			});
		}
	}

	private void removeReceiver(){
		try{
			if(registIntent != null){
				service.unregisterReceiver(alarmReceiver);
			}
		}catch (Exception e){
			// Nothing to do.
		}

	}
}
