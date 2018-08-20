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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;

import com.kt.smartview.support.log.YWMLog;
import com.kt.smartview.push.client.MqttTraceHandler;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class MqttAndroidClient implements IMqttAsyncClient {
	private final YWMLog logger = new YWMLog(MqttAndroidClient.class);
	public enum Ack {
		AUTO_ACK, MANUAL_ACK
	}

	private static ExecutorService pool = Executors.newSingleThreadExecutor();
	private MqttService mqttService;
	private String clientHandle;
	Context myContext;
	private SparseArray<IMqttToken> tokenMap = new SparseArray<IMqttToken>();
	private int tokenNumber = 0;

	// Connection data
	private String serverURI;
	private String clientId;
	private MqttClientPersistence persistence = null;
	private MqttConnectOptions connectOptions;
	private IMqttToken connectToken;

	private MqttCallback callback;
	private MqttTraceHandler traceCallback;

	// The acknowledgment that a message has been processed by the application
	private Ack messageAck;
	private boolean traceEnabled = false;

	public MqttAndroidClient(Context context, MqttService mqttService, String serverURI, String clientId) {
		this(context, mqttService, serverURI, clientId, null, Ack.AUTO_ACK);
	}

	public MqttAndroidClient(Context ctx, MqttService mqttService, String serverURI, String clientId, Ack ackType) {
		this(ctx, mqttService, serverURI, clientId, null, ackType);
	}

	public MqttAndroidClient(Context ctx, MqttService mqttService, String serverURI, String clientId, MqttClientPersistence persistence) {
		this(ctx, mqttService, serverURI, clientId, persistence, Ack.AUTO_ACK);
	}

	public MqttAndroidClient(Context context, MqttService mqttService, String serverURI, String clientId, MqttClientPersistence persistence,
			Ack ackType) {
		myContext = context;
		this.serverURI = serverURI;
		this.clientId = clientId;
		this.persistence = persistence;
		this.mqttService = mqttService;
		messageAck = ackType;
	}

	@Override
	public boolean isConnected() {
		if (mqttService != null)
			return mqttService.isConnected(clientHandle);
		else
			return false;
	}

	@Override
	public String getClientId() {
		return clientId;
	}

	@Override
	public String getServerURI() {
		return serverURI;
	}

	@Override
	public void close() {
		if (clientHandle == null) {
			clientHandle = mqttService.getClient(serverURI, clientId, myContext.getApplicationInfo().packageName,
					persistence);
		}
		mqttService.close(clientHandle);
	}

	@Override
	public IMqttToken connect() throws MqttException {
		return connect(null, null);
	}

	@Override
	public IMqttToken connect(MqttConnectOptions options) throws MqttException {
		return connect(options, null, null);
	}

	@Override
	public IMqttToken connect(Object userContext, IMqttActionListener callback) throws MqttException {
		return connect(new MqttConnectOptions(), userContext, callback);
	}

	@Override
	public IMqttToken connect(MqttConnectOptions options, Object userContext, IMqttActionListener callback)
			throws MqttException {

		IMqttToken token = new MqttTokenAndroid(this, userContext, callback);

		connectOptions = options;
		connectToken = token;
		pool.execute(new Runnable() {

			@Override
			public void run() {
				doConnect();
//				registerReceiver(MqttAndroidClient.this);
			}

		});
		return token;
	}

	private void doConnect() {
		if (clientHandle == null) {
			clientHandle = mqttService.getClient(serverURI, clientId, myContext.getApplicationInfo().packageName,
					persistence);
		}
		mqttService.setTraceEnabled(traceEnabled);
		mqttService.setTraceCallbackId(clientHandle);

		String activityToken = storeToken(connectToken);
		try {
			mqttService.connect(clientHandle, connectOptions, null, activityToken);
		} catch (MqttException e) {
			IMqttActionListener listener = connectToken.getActionCallback();
			if (listener != null) {
				listener.onFailure(connectToken, e);
			}
		}
	}

	@Override
	public IMqttToken disconnect() throws MqttException {
		IMqttToken token = new MqttTokenAndroid(this, null, null);
		String activityToken = storeToken(token);
		mqttService.disconnect(clientHandle, null, activityToken);
		return token;
	}

	@Override
	public IMqttToken disconnect(long quiesceTimeout) throws MqttException {
		IMqttToken token = new MqttTokenAndroid(this, null, null);
		String activityToken = storeToken(token);
		mqttService.disconnect(clientHandle, quiesceTimeout, null, activityToken);
		return token;
	}

	@Override
	public IMqttToken disconnect(Object userContext, IMqttActionListener callback) throws MqttException {
		IMqttToken token = new MqttTokenAndroid(this, userContext, callback);
		String activityToken = storeToken(token);
		mqttService.disconnect(clientHandle, null, activityToken);
		return token;
	}

	@Override
	public IMqttToken disconnect(long quiesceTimeout, Object userContext, IMqttActionListener callback)
			throws MqttException {
		IMqttToken token = new MqttTokenAndroid(this, userContext, callback);
		String activityToken = storeToken(token);
		mqttService.disconnect(clientHandle, quiesceTimeout, null, activityToken);
		return token;
	}

	@Override
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained)
			throws MqttException {
		return publish(topic, payload, qos, retained, null, null);
	}

	@Override
	public IMqttDeliveryToken publish(String topic, MqttMessage message)
			throws MqttException {
		return publish(topic, message, null, null);
	}

	@Override
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained, Object userContext,
			IMqttActionListener callback) throws MqttException {

		MqttMessage message = new MqttMessage(payload);
		message.setQos(qos);
		message.setRetained(retained);
		MqttDeliveryTokenAndroid token = new MqttDeliveryTokenAndroid(this, userContext, callback, message);
		String activityToken = storeToken(token);
		IMqttDeliveryToken internalToken = mqttService.publish(clientHandle, topic, payload, qos, retained, null,
				activityToken);
		token.setDelegate(internalToken);
		return token;
	}

	@Override
	public IMqttDeliveryToken publish(String topic, MqttMessage message, Object userContext,
			IMqttActionListener callback) throws MqttException {
		MqttDeliveryTokenAndroid token = new MqttDeliveryTokenAndroid(this, userContext, callback, message);
		String activityToken = storeToken(token);
		IMqttDeliveryToken internalToken = mqttService.publish(clientHandle, topic, message, null, activityToken);
		token.setDelegate(internalToken);
		return token;
	}

	@Override
	public IMqttToken subscribe(String topic, int qos) throws MqttException {
		return subscribe(topic, qos, null, null);
	}

	@Override
	public IMqttToken subscribe(String[] topic, int[] qos) throws MqttException {
		return subscribe(topic, qos, null, null);
	}

	@Override
	public IMqttToken subscribe(String topic, int qos, Object userContext, IMqttActionListener callback)
			throws MqttException {
		IMqttToken token = new MqttTokenAndroid(this, userContext, callback, new String[] { topic });
		String activityToken = storeToken(token);
		mqttService.subscribe(clientHandle, topic, qos, null, activityToken);
		return token;
	}

	@Override
	public IMqttToken subscribe(String[] topic, int[] qos, Object userContext, IMqttActionListener callback)
			throws MqttException {
		IMqttToken token = new MqttTokenAndroid(this, userContext, callback, topic);
		String activityToken = storeToken(token);
		mqttService.subscribe(clientHandle, topic, qos, null, activityToken);
		return token;
	}

	@Override
	public IMqttToken unsubscribe(String topic) throws MqttException {
		return unsubscribe(topic, null, null);
	}

	@Override
	public IMqttToken unsubscribe(String[] topic) throws MqttException {
		return unsubscribe(topic, null, null);
	}

	@Override
	public IMqttToken unsubscribe(String topic, Object userContext, IMqttActionListener callback) throws MqttException {
		IMqttToken token = new MqttTokenAndroid(this, userContext, callback);
		String activityToken = storeToken(token);
		mqttService.unsubscribe(clientHandle, topic, null, activityToken);
		return token;
	}

	@Override
	public IMqttToken unsubscribe(String[] topic, Object userContext, IMqttActionListener callback)
			throws MqttException {
		IMqttToken token = new MqttTokenAndroid(this, userContext, callback);
		String activityToken = storeToken(token);
		mqttService.unsubscribe(clientHandle, topic, null, activityToken);
		return token;
	}

	@Override
	public IMqttDeliveryToken[] getPendingDeliveryTokens() {
		return mqttService.getPendingDeliveryTokens(clientHandle);
	}

	@Override
	public void setCallback(MqttCallback callback) {
		this.callback = callback;

	}

	public void setTraceCallback(MqttTraceHandler traceCallback) {
		this.traceCallback = traceCallback;
		// mqttService.setTraceCallbackId(traceCallbackId);
	}

	public void setTraceEnabled(boolean traceEnabled) {
		this.traceEnabled = traceEnabled;
		if (mqttService != null)
			mqttService.setTraceEnabled(traceEnabled);
	}

	public void onActionReceived(Intent intent) {
		Bundle data = intent.getExtras();

		String handleFromIntent = data.getString(MqttServiceConstants.CALLBACK_CLIENT_HANDLE);

		if ((handleFromIntent == null) || (!handleFromIntent.equals(clientHandle))) {
			return;
		}

		String action = data.getString(MqttServiceConstants.CALLBACK_ACTION);

		if (MqttServiceConstants.CONNECT_ACTION.equals(action)) {
			connectAction(data);
		} else if (MqttServiceConstants.MESSAGE_ARRIVED_ACTION.equals(action)) {
			messageArrivedAction(data);
		} else if (MqttServiceConstants.SUBSCRIBE_ACTION.equals(action)) {
			subscribeAction(data);
		} else if (MqttServiceConstants.UNSUBSCRIBE_ACTION.equals(action)) {
			unSubscribeAction(data);
		} else if (MqttServiceConstants.SEND_ACTION.equals(action)) {
			sendAction(data);
		} else if (MqttServiceConstants.MESSAGE_DELIVERED_ACTION.equals(action)) {
			messageDeliveredAction(data);
		} else if (MqttServiceConstants.ON_CONNECTION_LOST_ACTION.equals(action)) {
			connectionLostAction(data);
		} else if (MqttServiceConstants.DISCONNECT_ACTION.equals(action)) {
			disconnected(data);
		} else if (MqttServiceConstants.TRACE_ACTION.equals(action)) {
			traceAction(data);
		} else {
			mqttService.traceError(MqttService.TAG, "Callback action doesn't exist.");
		}

	}

	public boolean acknowledgeMessage(String messageId) {
		if (messageAck == Ack.MANUAL_ACK) {
			Status status = mqttService.acknowledgeMessageArrival(clientHandle, messageId);
			return status == Status.OK;
		}
		return false;

	}

	private void connectAction(Bundle data) {
		IMqttToken token = connectToken;
		removeMqttToken(data);

		simpleAction(token, data);
	}

	private void disconnected(Bundle data) {
		clientHandle = null; // avoid reuse!
		IMqttToken token = removeMqttToken(data);
		if (token != null) {
			((MqttTokenAndroid) token).notifyComplete();
		}
		if (callback != null) {
			callback.connectionLost(null);
		}
	}

	private void connectionLostAction(Bundle data) {
		if (callback != null) {
			Exception reason = (Exception) data.getSerializable(MqttServiceConstants.CALLBACK_EXCEPTION);
			callback.connectionLost(reason);
		}
	}

	private void simpleAction(IMqttToken token, Bundle data) {
		if (token != null) {
			Status status = (Status) data.getSerializable(MqttServiceConstants.CALLBACK_STATUS);
			if (status == Status.OK) {
				((MqttTokenAndroid) token).notifyComplete();
			} else {
				Exception exceptionThrown = (Exception) data.getSerializable(MqttServiceConstants.CALLBACK_EXCEPTION);
				((MqttTokenAndroid) token).notifyFailure(exceptionThrown);
			}
		} else {
			mqttService.traceError(MqttService.TAG, "simpleAction : token is null");
		}
	}

	private void sendAction(Bundle data) {

		IMqttToken token = getMqttToken(data); // get, don't remove - will
		// remove on delivery
		logger.d("sendAction-->" + token);
		simpleAction(token, data);
	}

	private void subscribeAction(Bundle data) {
		IMqttToken token = removeMqttToken(data);
		simpleAction(token, data);
	}

	private void unSubscribeAction(Bundle data) {
		IMqttToken token = removeMqttToken(data);
		simpleAction(token, data);
	}

	private void messageDeliveredAction(Bundle data) {
		IMqttToken token = removeMqttToken(data);
		if (token != null) {
			if (callback != null) {
				Status status = (Status) data.getSerializable(MqttServiceConstants.CALLBACK_STATUS);
				if (status == Status.OK) {
					callback.deliveryComplete((IMqttDeliveryToken) token);
				}
			}
		}
	}

	private void messageArrivedAction(Bundle data) {
		if (callback != null) {
			String messageId = data.getString(MqttServiceConstants.CALLBACK_MESSAGE_ID);
			String destinationName = data.getString(MqttServiceConstants.CALLBACK_DESTINATION_NAME);

			ParcelableMqttMessage message = data
					.getParcelable(MqttServiceConstants.CALLBACK_MESSAGE_PARCEL);
			try {
				if (messageAck == Ack.AUTO_ACK) {
					callback.messageArrived(destinationName, message);
					mqttService.acknowledgeMessageArrival(clientHandle, messageId);
				} else {
					message.messageId = messageId;
					callback.messageArrived(destinationName, message);
				}

				// let the service discard the saved message details
			} catch (Exception e) {
				// Swallow the exception
			}
		}
	}

	private void traceAction(Bundle data) {

		if (traceCallback != null) {
			String severity = data.getString(MqttServiceConstants.CALLBACK_TRACE_SEVERITY);
			String message = data.getString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE);
			String tag = data.getString(MqttServiceConstants.CALLBACK_TRACE_TAG);
			if (MqttServiceConstants.TRACE_DEBUG.equals(severity))
				traceCallback.traceDebug(tag, message);
			else if (MqttServiceConstants.TRACE_ERROR.equals(severity))
				traceCallback.traceError(tag, message);
			else {
				Exception e = (Exception) data.getSerializable(MqttServiceConstants.CALLBACK_EXCEPTION);
				traceCallback.traceException(tag, message, e);
			}
		}
	}

	private synchronized String storeToken(IMqttToken token) {
		tokenMap.put(tokenNumber, token);
		return Integer.toString(tokenNumber++);
	}

	private synchronized IMqttToken removeMqttToken(Bundle data) {

		String activityToken = data.getString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN);
		if (activityToken != null) {
			int tokenNumber = Integer.parseInt(activityToken);
			IMqttToken token = tokenMap.get(tokenNumber);
			tokenMap.delete(tokenNumber);
			return token;
		}
		return null;
	}

	private synchronized IMqttToken getMqttToken(Bundle data) {
		String activityToken = data.getString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN);
		IMqttToken token = tokenMap.get(Integer.parseInt(activityToken));
		return token;
	}

	public SSLSocketFactory getSSLSocketFactory(InputStream keyStore, String password) throws MqttSecurityException {
		try {
			SSLContext ctx = null;
			SSLSocketFactory sslSockFactory = null;
			KeyStore ts;
			ts = KeyStore.getInstance("BKS");
			ts.load(keyStore, password.toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
			tmf.init(ts);
			TrustManager[] tm = tmf.getTrustManagers();
			ctx = SSLContext.getInstance("SSL");
			ctx.init(null, tm, null);

			sslSockFactory = ctx.getSocketFactory();
			return sslSockFactory;

		} catch (KeyStoreException e) {
			throw new MqttSecurityException(e);
		} catch (CertificateException e) {
			throw new MqttSecurityException(e);
		} catch (FileNotFoundException e) {
			throw new MqttSecurityException(e);
		} catch (IOException e) {
			throw new MqttSecurityException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new MqttSecurityException(e);
		} catch (KeyManagementException e) {
			throw new MqttSecurityException(e);
		}
	}

	/*import java.io.*;
	import java.nio.file.*;
	import java.security.*;
	import java.security.cert.*;
	import java.security.interfaces.*;
	import javax.net.ssl.*;

	import org.bouncycastle.jce.provider.*;
	import org.bouncycastle.openssl.*;*/
/*
	static SSLSocketFactory getSocketFactory (final String caCrtFile, final String crtFile, final String keyFile, final String password) throws Exception
	{
		Security.addProvider(new BouncyCastleProvider());

		// load CA certificate
		PEMReader reader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(caCrtFile)))));
		X509Certificate caCert = (X509Certificate)reader.readObject();
		reader.close();

		// load client certificate
		reader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(crtFile)))));
		X509Certificate cert = (X509Certificate)reader.readObject();
		reader.close();

		// load client private key
		reader = new PEMReader(
				new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(keyFile)))),
				new PasswordFinder() {
					public char[] getPassword() {
						return password.toCharArray();
					}
				}
		);
		KeyPair key = (KeyPair)reader.readObject();
		reader.close();

		// CA certificate is used to authenticate server
		KeyStore caKs = KeyStore.getInstance("JKS");
		caKs.load(null, null);
		caKs.setCertificateEntry("ca-certificate", caCert);
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
		tmf.init(caKs);

		// client key and certificates are sent to server so it can authenticate us
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(null, null);
		ks.setCertificateEntry("certificate", cert);
		ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(), new java.security.cert.Certificate[]{cert});
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
		kmf.init(ks, password.toCharArray());

		// finally, create SSL socket factory
		SSLContext context = SSLContext.getInstance("TLSv1");
		context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		return context.getSocketFactory();
	}*/

	@Override
	public void disconnectForcibly() throws MqttException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void disconnectForcibly(long disconnectTimeout) throws MqttException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException {
		throw new UnsupportedOperationException();
	}

	public void reconnect(){
		if(mqttService != null){
			mqttService.reconnect(clientHandle);
		}
	}

	public MqttService getMqttService(){
		return mqttService;
	}
}
