/*******************************************************************************
 * Copyright (c) 1999, 2014 IBM Corp.
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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.kt.smartview.support.log.YWMLog;
import com.kt.smartview.push.PushAdapter;
import com.kt.smartview.push.client.MqttTraceHandler;
import com.kt.smartview.support.preference.SmartViewPreference;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MqttService extends Service implements MqttTraceHandler {
	private final YWMLog logger = new YWMLog(MqttService.class);
	static final String TAG = "MqttService";
	private String traceCallbackId;
	private boolean traceEnabled = false;
	MessageStore messageStore;
	private NetworkConnectionIntentReceiver networkConnectionMonitor;
	private BackgroundDataPreferenceReceiver backgroundDataPreferenceMonitor;
	private volatile boolean backgroundDataEnabled = true;
//	private MqttServiceBinder mqttServiceBinder;
	private Map<String/* clientHandle */, MqttConnection/* client */> connections = new ConcurrentHashMap<String, MqttConnection>();
	public MqttService() {
		super();
	}
	public abstract MqttAndroidClient getMqttAndroidClient();
	public abstract PushAdapter getPushAdapter();
	public abstract void scheduleReconnect();
	public abstract void cancelReconnect();
	public abstract  String getClientHandle();
	void callbackToActivity(String clientHandle, Status status, Bundle dataBundle) {
		Intent callbackIntent = new Intent(MqttServiceConstants.CALLBACK_TO_ACTIVITY);
		if (clientHandle != null) {
			callbackIntent.putExtra(MqttServiceConstants.CALLBACK_CLIENT_HANDLE, clientHandle);
		}
		callbackIntent.putExtra(MqttServiceConstants.CALLBACK_STATUS, status);
		if (dataBundle != null) {
			callbackIntent.putExtras(dataBundle);
		}
		MqttAndroidClient client = getMqttAndroidClient();
		if(client != null){
			client.onActionReceived(callbackIntent);
		}
//		LocalBroadcastManager.getInstance(this).sendBroadcast(callbackIntent);
	}

	public String getClient(String serverURI, String clientId, String contextId, MqttClientPersistence persistence) {
		String clientHandle = serverURI + ":" + clientId + ":" + contextId;
		if (!connections.containsKey(clientHandle)) {
			MqttConnection client = new MqttConnection(this, serverURI, clientId, persistence, clientHandle);
			connections.put(clientHandle, client);
		}
		return clientHandle;
	}

	public void connect(String clientHandle, MqttConnectOptions connectOptions, String invocationContext,
			String activityToken) throws MqttException {
		MqttConnection client = getConnection(clientHandle);
		client.connect(connectOptions, invocationContext, activityToken);

	}

	void reconnect(boolean isForce) {
		traceDebug(TAG, "Reconnect to server, client size=" + connections.size());
		for (MqttConnection client : connections.values()) {
			traceDebug("Reconnect Client:", client.getClientId() + '/' + client.getServerURI());
			if (this.isOnline() && client != null && (isForce == false && client.isConnected() == false)) {
				client.reconnect();
			}
		}
	}
	void reconnect(String clientHandle) {
		MqttConnection client = getConnection(clientHandle);
		if (client != null && this.isOnline()) {
			client.reconnect();
		}
	}
	public void close(String clientHandle) {
		MqttConnection client = getConnection(clientHandle);
		client.close();
	}

	public void disconnect(String clientHandle, String invocationContext, String activityToken) {
		MqttConnection client = getConnection(clientHandle);
		client.disconnect(invocationContext, activityToken);
		connections.remove(clientHandle);
		stopSelf();
	}

	public void disconnect(String clientHandle, long quiesceTimeout, String invocationContext, String activityToken) {
		MqttConnection client = getConnection(clientHandle);
		client.disconnect(quiesceTimeout, invocationContext, activityToken);
		connections.remove(clientHandle);
		stopSelf();
	}

	public boolean isConnected(String clientHandle) {
		MqttConnection client = getConnection(clientHandle);
		return client.isConnected();
	}

	public IMqttDeliveryToken publish(String clientHandle, String topic, byte[] payload, int qos, boolean retained,
			String invocationContext, String activityToken) throws MqttException {
		MqttConnection client = getConnection(clientHandle);
		return client.publish(topic, payload, qos, retained, invocationContext, activityToken);
	}

	public IMqttDeliveryToken publish(String clientHandle, String topic, MqttMessage message, String invocationContext,
			String activityToken) throws MqttException {
		MqttConnection client = getConnection(clientHandle);
		return client.publish(topic, message, invocationContext, activityToken);
	}

	public void subscribe(String clientHandle, String topic, int qos, String invocationContext, String activityToken) {
		MqttConnection client = getConnection(clientHandle);
		client.subscribe(topic, qos, invocationContext, activityToken);
	}

	public void subscribe(String clientHandle, String[] topic, int[] qos, String invocationContext,
			String activityToken) {
		MqttConnection client = getConnection(clientHandle);
		client.subscribe(topic, qos, invocationContext, activityToken);
	}

	public void unsubscribe(String clientHandle, final String topic, String invocationContext, String activityToken) {
		MqttConnection client = getConnection(clientHandle);
		client.unsubscribe(topic, invocationContext, activityToken);
	}

	public void unsubscribe(String clientHandle, final String[] topic, String invocationContext, String activityToken) {
		MqttConnection client = getConnection(clientHandle);
		client.unsubscribe(topic, invocationContext, activityToken);
	}

	public IMqttDeliveryToken[] getPendingDeliveryTokens(String clientHandle) {
		MqttConnection client = getConnection(clientHandle);
		return client.getPendingDeliveryTokens();
	}

	private MqttConnection getConnection(String clientHandle) {
		MqttConnection client = connections.get(clientHandle);
		if (client == null) {
			throw new IllegalArgumentException("Invalid ClientHandle");
		}
		return client;
	}

	public Status acknowledgeMessageArrival(String clientHandle, String id) {
		if (messageStore.discardArrived(clientHandle, id)) {
			return Status.OK;
		} else {
			return Status.ERROR;
		}
	}

	// Extend Service

	@Override
	public void onCreate() {
		super.onCreate();
		messageStore = new DatabaseMessageStore(this, this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterBroadcastReceivers();
		for (MqttConnection client : connections.values()) {
			client.disconnect(null, null);
		}
		if (this.messageStore != null)
			this.messageStore.close();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(final Intent intent, int flags, final int startId) {
		super.onStartCommand(intent, flags, startId);
		if(SmartViewPreference.isPushOn(getApplicationContext()) == false){
			return START_STICKY;
		}
		registerBroadcastReceivers();
		return START_STICKY;
	}

	public void setTraceCallbackId(String traceCallbackId) {
		this.traceCallbackId = traceCallbackId;
	}

	public void setTraceEnabled(boolean traceEnabled) {
		this.traceEnabled = traceEnabled;
	}

	public boolean isTraceEnabled() {
		return this.traceEnabled;
	}

	@Override
	public void traceDebug(String tag, String message) {
		traceCallback(MqttServiceConstants.TRACE_DEBUG, tag, message);
	}

	@Override
	public void traceError(String tag, String message) {
		traceCallback(MqttServiceConstants.TRACE_ERROR, tag, message);
	}

	private void traceCallback(String severity, String tag, String message) {
		if ((traceCallbackId != null) && (traceEnabled)) {
			Bundle dataBundle = new Bundle();
			dataBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.TRACE_ACTION);
			dataBundle.putString(MqttServiceConstants.CALLBACK_TRACE_SEVERITY, severity);
			dataBundle.putString(MqttServiceConstants.CALLBACK_TRACE_TAG, tag);
			// dataBundle.putString(MqttServiceConstants.CALLBACK_TRACE_ID,
			// traceCallbackId);
			dataBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, message);
			callbackToActivity(traceCallbackId, Status.ERROR, dataBundle);
		}
	}

	@Override
	public void traceException(String tag, String message, Exception e) {
		if (traceCallbackId != null) {
			Bundle dataBundle = new Bundle();
			dataBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.TRACE_ACTION);
			dataBundle.putString(MqttServiceConstants.CALLBACK_TRACE_SEVERITY, MqttServiceConstants.TRACE_EXCEPTION);
			dataBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, message);
			dataBundle.putSerializable(MqttServiceConstants.CALLBACK_EXCEPTION, e); // TODO:
																					// Check
			dataBundle.putString(MqttServiceConstants.CALLBACK_TRACE_TAG, tag);
			// dataBundle.putString(MqttServiceConstants.CALLBACK_TRACE_ID,
			// traceCallbackId);
			callbackToActivity(traceCallbackId, Status.ERROR, dataBundle);
		}
	}

	@SuppressWarnings("deprecation")
	public void registerBroadcastReceivers() {
		if (networkConnectionMonitor == null) {
			networkConnectionMonitor = new NetworkConnectionIntentReceiver();
			registerReceiver(networkConnectionMonitor, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		}

		if (Build.VERSION.SDK_INT < 14 /**
										 * Build.VERSION_CODES.
										 * ICE_CREAM_SANDWICH
										 **/
		) {
			// Support the old system for background data preferences
			ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			backgroundDataEnabled = cm.getBackgroundDataSetting();
			if (backgroundDataPreferenceMonitor == null) {
				backgroundDataPreferenceMonitor = new BackgroundDataPreferenceReceiver();
				registerReceiver(backgroundDataPreferenceMonitor,
						new IntentFilter(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));
			}
		}
	}

	public void unregisterBroadcastReceivers() {
		if (networkConnectionMonitor != null) {
			unregisterReceiver(networkConnectionMonitor);
			networkConnectionMonitor = null;
		}

		if (Build.VERSION.SDK_INT < 14 /**
										 * Build.VERSION_CODES.
										 * ICE_CREAM_SANDWICH
										 **/
		) {
			if (backgroundDataPreferenceMonitor != null) {
				unregisterReceiver(backgroundDataPreferenceMonitor);
			}
		}
	}

	private class NetworkConnectionIntentReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			logger.d("Internal network status receive.");
			if(SmartViewPreference.isPushOn(getApplicationContext()) == false){
				return;
			}
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
			wl.acquire();
			if(isOnline()){
				logger.d("Online,reconnect.");
				reconnect(false);
			}else{
				logger.d("disconnected.... notifyClientsOffline");
				notifyClientsOffline();
			}
			wl.release();
		}
	}

	/**
	 * @return whether the android service can be regarded as online
	 */
	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if(cm != null){
			return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable()
					&& cm.getActiveNetworkInfo().isConnected() && backgroundDataEnabled;
		}else{
			return false;
		}
	}

	/**
	 * Notify clients we're offline
	 */
	public void notifyClientsOffline() {
		for (MqttConnection connection : connections.values()) {
			connection.offline();
		}
	}

	/**
	 * Detect changes of the Allow Background Data setting - only used below
	 * ICE_CREAM_SANDWICH
	 */
	private class BackgroundDataPreferenceReceiver extends BroadcastReceiver {

		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			traceDebug(TAG, "Reconnect since BroadcastReceiver.");
			if (cm.getBackgroundDataSetting()) {
				if (!backgroundDataEnabled) {
					backgroundDataEnabled = true;
					// we have the Internet connection - have another try at
					// connecting
					reconnect(false);
				}
			} else {
				backgroundDataEnabled = false;
				notifyClientsOffline();
			}
		}
	}

}
