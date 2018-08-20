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
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.kt.smartview.support.log.YWMLog;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class MqttConnection implements MqttCallback {
	private final YWMLog logger = new YWMLog(this.getClass());
	private static final String TAG = "MqttConnection";
	private static final String NOT_CONNECTED = "not connected";
	private String serverURI;
	private String clientId;
	private MqttClientPersistence persistence = null;
	private MqttConnectOptions connectOptions;

	public String getServerURI() {
		return serverURI;
	}

	public void setServerURI(String serverURI) {
		this.serverURI = serverURI;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public MqttConnectOptions getConnectOptions() {
		return connectOptions;
	}

	public void setConnectOptions(MqttConnectOptions connectOptions) {
		this.connectOptions = connectOptions;
	}

	// Client handle, used for callbacks...
	private String clientHandle;

	public String getClientHandle() {
		return clientHandle;
	}

	public void setClientHandle(String clientHandle) {
		this.clientHandle = clientHandle;
	}

	// store connect ActivityToken for reconnect
	private String reconnectActivityToken = null;

	// our client object - instantiated on connect
	private MqttAsyncClient myClient = null;

	// our (parent) service object
	private MqttService service = null;

	private volatile boolean disconnected = true;
	private boolean cleanSession = true;

	// Indicate this connection is connecting or not.
	// This variable uses to avoid reconnect multiple times.
	private volatile boolean isConnecting = false;

	// Saved sent messages and their corresponding Topics, activityTokens and
	// invocationContexts, so we can handle "deliveryComplete" callbacks
	// from the mqttClient
	private Map<IMqttDeliveryToken, String /* Topic */> savedTopics = new HashMap<IMqttDeliveryToken, String>();
	private Map<IMqttDeliveryToken, MqttMessage> savedSentMessages = new HashMap<IMqttDeliveryToken, MqttMessage>();
	private Map<IMqttDeliveryToken, String> savedActivityTokens = new HashMap<IMqttDeliveryToken, String>();
	private Map<IMqttDeliveryToken, String> savedInvocationContexts = new HashMap<IMqttDeliveryToken, String>();

	private WakeLock wakelock = null;
	private String wakeLockTag = null;

	MqttConnection(MqttService service, String serverURI, String clientId, MqttClientPersistence persistence,
			String clientHandle) {
		this.serverURI = serverURI.toString();
		this.service = service;
		this.clientId = clientId;
		this.persistence = persistence;
		this.clientHandle = clientHandle;

		StringBuffer buff = new StringBuffer(this.getClass().getCanonicalName());
		buff.append(" ");
		buff.append(clientId);
		buff.append(" ");
		buff.append("on host ");
		buff.append(serverURI);
		wakeLockTag = buff.toString();
	}

	public void connect(MqttConnectOptions options, String invocationContext, String activityToken) {

		connectOptions = options;
		reconnectActivityToken = activityToken;

		if (options != null) {
			cleanSession = options.isCleanSession();
		}

		if (connectOptions.isCleanSession()) { // if it's a clean session,
			// discard old data
			service.messageStore.clearArrivedMessages(clientHandle);
		}

		service.traceDebug(TAG, "Connecting {" + serverURI + "} as {" + clientId + "}");
		final Bundle resultBundle = new Bundle();
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN, activityToken);
		resultBundle.putString(MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT, invocationContext);
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.CONNECT_ACTION);

		try {
			if (persistence == null) {
				// ask Android where we can put files
				File myDir = service.getExternalFilesDir(TAG);

				if (myDir == null) {
					// No external storage, use internal storage instead.
					myDir = service.getDir(TAG, Context.MODE_PRIVATE);

					if (myDir == null) {
						// Shouldn't happen.
						resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
								"Error! No external and internal storage available");
						resultBundle.putSerializable(MqttServiceConstants.CALLBACK_EXCEPTION,
								new MqttPersistenceException());
						service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
						return;
					}
				}

				// use that to setup MQTT client persistence storage
				persistence = new MqttDefaultFilePersistence(myDir.getAbsolutePath());
			}

			IMqttActionListener listener = new MqttConnectionListener(resultBundle) {

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					doAfterConnectSuccess(resultBundle);
					service.traceDebug(TAG, "connect success!");
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
							exception.getLocalizedMessage());
					resultBundle.putSerializable(MqttServiceConstants.CALLBACK_EXCEPTION, exception);
					service.traceError(TAG, "connect fail, call connect to reconnect.reason:" + exception.getMessage());

					doAfterConnectFail(resultBundle);

				}
			};

			if (myClient != null) {
				if (isConnecting) {
					service.traceDebug(TAG, "myClient != null and the client is connecting. Connect return directly.");
					service.traceDebug(TAG,
							"Connect return:isConnecting:" + isConnecting + ".disconnected:" + disconnected);
					return;
				} else if (!disconnected) {
					service.traceDebug(TAG, "myClient != null and the client is connected and notify!");
					doAfterConnectSuccess(resultBundle);
				} else {
					service.traceDebug(TAG, "myClient != null and the client is not connected");
					service.traceDebug(TAG, "Do Real connect!");
					setConnectingState(true);
					myClient.connect(connectOptions, invocationContext, listener);
				}
			}

			// if myClient is null, then create a new connection
			else {

				myClient = new MqttAsyncClient(serverURI, clientId, persistence, new AlarmPingSender(service));
				myClient.setCallback(this);

				service.traceDebug(TAG, "Do Real connect!");
				setConnectingState(true);
				myClient.connect(connectOptions, invocationContext, listener);
			}
		} catch (Exception e) {
			handleException(resultBundle, e);
		}
	}

	private void doAfterConnectSuccess(final Bundle resultBundle) {
		// since the device's cpu can go to sleep, acquire a wakelock and drop
		// it later.
		acquireWakeLock();
		service.callbackToActivity(clientHandle, Status.OK, resultBundle);
		deliverBacklog();
		setConnectingState(false);
		disconnected = false;
		releaseWakeLock();
	}

	private void doAfterConnectFail(final Bundle resultBundle) {
		//
		acquireWakeLock();
		disconnected = true;
		setConnectingState(false);
		service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
		releaseWakeLock();
	}

	private void handleException(final Bundle resultBundle, Exception e) {
		resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, e.getLocalizedMessage());

		resultBundle.putSerializable(MqttServiceConstants.CALLBACK_EXCEPTION, e);

		service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
	}

	/**
	 * Attempt to deliver any outstanding messages we've received but which the
	 * application hasn't acknowledged. If "cleanSession" was specified, we'll
	 * have already purged any such messages from our messageStore.
	 */
	private void deliverBacklog() {
		Iterator<MessageStore.StoredMessage> backlog = service.messageStore.getAllArrivedMessages(clientHandle);
		while (backlog.hasNext()) {
			MessageStore.StoredMessage msgArrived = backlog.next();
			Bundle resultBundle = messageToBundle(msgArrived.getMessageId(), msgArrived.getTopic(),
					msgArrived.getMessage());
			resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.MESSAGE_ARRIVED_ACTION);
			service.callbackToActivity(clientHandle, Status.OK, resultBundle);
		}
	}

	private Bundle messageToBundle(String messageId, String topic, MqttMessage message) {
		Bundle result = new Bundle();
		result.putString(MqttServiceConstants.CALLBACK_MESSAGE_ID, messageId);
		result.putString(MqttServiceConstants.CALLBACK_DESTINATION_NAME, topic);
		result.putParcelable(MqttServiceConstants.CALLBACK_MESSAGE_PARCEL, new ParcelableMqttMessage(message));
		return result;
	}

	void close() {
		service.traceDebug(TAG, "close()");
		try {
			if (myClient != null) {
				myClient.close();
			}
		} catch (MqttException e) {
			// Pass a new bundle, let handleException stores error messages.
			handleException(new Bundle(), e);
		}
	}

	void disconnect(long quiesceTimeout, String invocationContext, String activityToken) {
		service.traceDebug(TAG, "disconnect()");
		disconnected = true;
		final Bundle resultBundle = new Bundle();
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN, activityToken);
		resultBundle.putString(MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT, invocationContext);
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.DISCONNECT_ACTION);
		if ((myClient != null) && (myClient.isConnected())) {
			IMqttActionListener listener = new MqttConnectionListener(resultBundle);
			try {
				myClient.disconnect(quiesceTimeout, invocationContext, listener);
			} catch (Exception e) {
				handleException(resultBundle, e);
			}
		} else {
			resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, NOT_CONNECTED);
			service.traceError(MqttServiceConstants.DISCONNECT_ACTION, NOT_CONNECTED);
			service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
		}

		if (connectOptions.isCleanSession()) {
			// assume we'll clear the stored messages at this point
			service.messageStore.clearArrivedMessages(clientHandle);
		}

		releaseWakeLock();
	}

	void disconnect(String invocationContext, String activityToken) {
		service.traceDebug(TAG, "disconnect()");
		disconnected = true;
		final Bundle resultBundle = new Bundle();
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN, activityToken);
		resultBundle.putString(MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT, invocationContext);
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.DISCONNECT_ACTION);
		if ((myClient != null) && (myClient.isConnected())) {
			IMqttActionListener listener = new MqttConnectionListener(resultBundle);
			try {
				myClient.disconnect(invocationContext, listener);
			} catch (Exception e) {
				handleException(resultBundle, e);
			}
		} else {
			resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, NOT_CONNECTED);
			service.traceError(MqttServiceConstants.DISCONNECT_ACTION, NOT_CONNECTED);
			service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
		}

		if (connectOptions.isCleanSession()) {
			// assume we'll clear the stored messages at this point
			service.messageStore.clearArrivedMessages(clientHandle);
		}
		releaseWakeLock();
	}

	public boolean isConnected() {
		if (myClient != null)
			return myClient.isConnected();
		return false;
	}

	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained, String invocationContext,
			String activityToken) {
		final Bundle resultBundle = new Bundle();
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.SEND_ACTION);
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN, activityToken);
		resultBundle.putString(MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT, invocationContext);

		IMqttDeliveryToken sendToken = null;

		if ((myClient != null) && (myClient.isConnected())) {
			IMqttActionListener listener = new MqttConnectionListener(resultBundle);
			try {
				MqttMessage message = new MqttMessage(payload);
				message.setQos(qos);
				message.setRetained(retained);
				sendToken = myClient.publish(topic, payload, qos, retained, invocationContext, listener);
				storeSendDetails(topic, message, sendToken, invocationContext, activityToken);
			} catch (Exception e) {
				handleException(resultBundle, e);
			}
		} else {
			resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, NOT_CONNECTED);
			service.traceError(MqttServiceConstants.SEND_ACTION, NOT_CONNECTED);
			service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
		}

		return sendToken;
	}

	public IMqttDeliveryToken publish(String topic, MqttMessage message, String invocationContext,
			String activityToken) {
		final Bundle resultBundle = new Bundle();
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.SEND_ACTION);
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN, activityToken);
		resultBundle.putString(MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT, invocationContext);

		IMqttDeliveryToken sendToken = null;

		if ((myClient != null) && (myClient.isConnected())) {
			IMqttActionListener listener = new MqttConnectionListener(resultBundle);
			try {
				sendToken = myClient.publish(topic, message, invocationContext, listener);
				storeSendDetails(topic, message, sendToken, invocationContext, activityToken);
			} catch (Exception e) {
				handleException(resultBundle, e);
			}
		} else {
			resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, NOT_CONNECTED);
			service.traceError(MqttServiceConstants.SEND_ACTION, NOT_CONNECTED);
			service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
		}
		return sendToken;
	}

	public void subscribe(final String topic, final int qos, String invocationContext, String activityToken) {
		service.traceDebug(TAG,
				"subscribe({" + topic + "}," + qos + ",{" + invocationContext + "}, {" + activityToken + "}");
		final Bundle resultBundle = new Bundle();
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.SUBSCRIBE_ACTION);
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN, activityToken);
		resultBundle.putString(MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT, invocationContext);

		if ((myClient != null) && (myClient.isConnected())) {
			IMqttActionListener listener = new MqttConnectionListener(resultBundle);
			try {
				myClient.subscribe(topic, qos, invocationContext, listener);
			} catch (Exception e) {
				handleException(resultBundle, e);
			}
		} else {
			resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, NOT_CONNECTED);
			service.traceError("subscribe", NOT_CONNECTED);
			service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
		}
	}

	public void subscribe(final String[] topic, final int[] qos, String invocationContext, String activityToken) {
		service.traceDebug(TAG,
				"subscribe({" + topic + "}," + qos + ",{" + invocationContext + "}, {" + activityToken + "}");
		final Bundle resultBundle = new Bundle();
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.SUBSCRIBE_ACTION);
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN, activityToken);
		resultBundle.putString(MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT, invocationContext);

		if ((myClient != null) && (myClient.isConnected())) {
			IMqttActionListener listener = new MqttConnectionListener(resultBundle);
			try {
				myClient.subscribe(topic, qos, invocationContext, listener);
			} catch (Exception e) {
				handleException(resultBundle, e);
			}
		} else {
			resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, NOT_CONNECTED);
			service.traceError("subscribe", NOT_CONNECTED);
			service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
		}
	}

	void unsubscribe(final String topic, String invocationContext, String activityToken) {
		service.traceDebug(TAG, "unsubscribe({" + topic + "},{" + invocationContext + "}, {" + activityToken + "})");
		final Bundle resultBundle = new Bundle();
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.UNSUBSCRIBE_ACTION);
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN, activityToken);
		resultBundle.putString(MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT, invocationContext);
		if ((myClient != null) && (myClient.isConnected())) {
			IMqttActionListener listener = new MqttConnectionListener(resultBundle);
			try {
				myClient.unsubscribe(topic, invocationContext, listener);
			} catch (Exception e) {
				handleException(resultBundle, e);
			}
		} else {
			resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, NOT_CONNECTED);

			service.traceError("subscribe", NOT_CONNECTED);
			service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
		}
	}

	void unsubscribe(final String[] topic, String invocationContext, String activityToken) {
		service.traceDebug(TAG, "unsubscribe({" + topic + "},{" + invocationContext + "}, {" + activityToken + "})");
		final Bundle resultBundle = new Bundle();
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.UNSUBSCRIBE_ACTION);
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN, activityToken);
		resultBundle.putString(MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT, invocationContext);
		if ((myClient != null) && (myClient.isConnected())) {
			IMqttActionListener listener = new MqttConnectionListener(resultBundle);
			try {
				myClient.unsubscribe(topic, invocationContext, listener);
			} catch (Exception e) {
				handleException(resultBundle, e);
			}
		} else {
			resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, NOT_CONNECTED);

			service.traceError("subscribe", NOT_CONNECTED);
			service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
		}
	}

	public IMqttDeliveryToken[] getPendingDeliveryTokens() {
		return myClient.getPendingDeliveryTokens();
	}

	// Implement MqttCallback
	@Override
	public void connectionLost(Throwable why) {
		service.traceDebug(TAG, "connectionLost(" + why.getMessage() + ")");
		disconnected = true;
		try {
			myClient.disconnect(null, new IMqttActionListener() {

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					// No action
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					// No action
				}
			});
		} catch (Exception e) {
			// ignore it - we've done our best
		}

		Bundle resultBundle = new Bundle();
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.ON_CONNECTION_LOST_ACTION);
		if (why != null) {
			resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, why.getMessage());
			if (why instanceof MqttException) {
				resultBundle.putSerializable(MqttServiceConstants.CALLBACK_EXCEPTION, why);
			}
			resultBundle.putString(MqttServiceConstants.CALLBACK_EXCEPTION_STACK, Log.getStackTraceString(why));
		}
		service.callbackToActivity(clientHandle, Status.OK, resultBundle);
		// client has lost connection no need for wake lock
		releaseWakeLock();
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken messageToken) {

		try {
			if(messageToken.getMessage() != null){
				service.traceDebug(TAG, "deliveryComplete(" + messageToken.getTopics() + ")");
			}
		} catch (MqttException e) {
			e.printStackTrace();
		}

		MqttMessage message = savedSentMessages.remove(messageToken);
		if (message != null) { // If I don't know about the message, it's
			// irrelevant
			String topic = savedTopics.remove(messageToken);
			String activityToken = savedActivityTokens.remove(messageToken);
			String invocationContext = savedInvocationContexts.remove(messageToken);

			Bundle resultBundle = messageToBundle(null, topic, message);
			if (activityToken != null) {
				resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.SEND_ACTION);
				resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN, activityToken);
				resultBundle.putString(MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT, invocationContext);

				service.callbackToActivity(clientHandle, Status.OK, resultBundle);
			}
			resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.MESSAGE_DELIVERED_ACTION);
			service.callbackToActivity(clientHandle, Status.OK, resultBundle);
		}

		// this notification will have kept the connection alive but send the
		// previously sechudled ping anyway
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {


		service.traceDebug(TAG, "messageArrived(" + topic + ",{" + message.toString() + "})");

		String messageId = service.messageStore.storeArrived(clientHandle, topic, message);

		Bundle resultBundle = messageToBundle(messageId, topic, message);
		resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.MESSAGE_ARRIVED_ACTION);
		resultBundle.putString(MqttServiceConstants.CALLBACK_MESSAGE_ID, messageId);
		service.callbackToActivity(clientHandle, Status.OK, resultBundle);

	}

	private void storeSendDetails(final String topic, final MqttMessage msg, final IMqttDeliveryToken messageToken,
			final String invocationContext, final String activityToken) {
		savedTopics.put(messageToken, topic);
		savedSentMessages.put(messageToken, msg);
		savedActivityTokens.put(messageToken, activityToken);
		savedInvocationContexts.put(messageToken, invocationContext);
	}

	/**
	 * Acquires a partial wake lock for this client
	 */
	private void acquireWakeLock() {
		if (wakelock == null) {
			PowerManager pm = (PowerManager) service.getSystemService(Service.POWER_SERVICE);
			wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
		}
		wakelock.acquire();

	}

	/**
	 * Releases the currently held wake lock for this client
	 */
	private void releaseWakeLock() {
		if (wakelock != null && wakelock.isHeld()) {
			wakelock.release();
		}
	}

	private class MqttConnectionListener implements IMqttActionListener {

		private final Bundle resultBundle;

		private MqttConnectionListener(Bundle resultBundle) {
			this.resultBundle = resultBundle;
		}

		@Override
		public void onSuccess(IMqttToken asyncActionToken) {
			service.callbackToActivity(clientHandle, Status.OK, resultBundle);
		}

		@Override
		public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, exception.getLocalizedMessage());

			resultBundle.putSerializable(MqttServiceConstants.CALLBACK_EXCEPTION, exception);

			service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
		}
	}

	/**
	 * Receive notification that we are offline<br>
	 * if cleanSession is true, we need to regard this as a disconnection
	 */
	void offline() {

		if (!disconnected && !cleanSession) {
			Exception e = new Exception("Android offline");
			connectionLost(e);
		}
	}

	/**
	 * Reconnect<br>
	 * Only appropriate if cleanSession is false and we were connected. Declare
	 * as synchronized to avoid multiple calls to this method to send connect
	 * multiple times
	 */
	synchronized void reconnect() {
		if(myClient == null){
			service.traceDebug(TAG, "The client is null.");
			return;
		}

		if (isConnecting) {
			service.traceDebug(TAG, "The client is connecting. Reconnect return directly.");
			return;
		}

		if (!service.isOnline()) {
			service.traceDebug(TAG, "The network is not reachable. Will not do reconnect");
			return;
		}

		if (disconnected && !cleanSession) {
			// use the activityToke the same with action connect
			service.traceDebug(TAG, "Do Real Reconnect!");
			final Bundle resultBundle = new Bundle();
			resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN, reconnectActivityToken);
			resultBundle.putString(MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT, null);
			resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.CONNECT_ACTION);

			try {

				IMqttActionListener listener = new MqttConnectionListener(resultBundle) {
					@Override
					public void onSuccess(IMqttToken asyncActionToken) {
						// since the device's cpu can go to sleep, acquire a
						// wakelock and drop it later.
						service.traceDebug(TAG, "Reconnect Success!");
						service.traceDebug(TAG, "DeliverBacklog when reconnect.");
						doAfterConnectSuccess(resultBundle);
					}

					@Override
					public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
						resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
								exception.getLocalizedMessage());
						resultBundle.putSerializable(MqttServiceConstants.CALLBACK_EXCEPTION, exception);
						service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);

						doAfterConnectFail(resultBundle);

					}
				};

				myClient.connect(connectOptions, null, listener);
				setConnectingState(true);
			} catch (MqttException e) {
				service.traceError(TAG, "Cannot reconnect to remote server." + e.getMessage());
				setConnectingState(false);
				handleException(resultBundle, e);
			}
		}
	}

	synchronized void setConnectingState(boolean isConnecting) {
		this.isConnecting = isConnecting;
	}
}
