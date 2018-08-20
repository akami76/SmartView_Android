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
package com.kt.smartview.push.client;

import android.content.Context;
import android.text.Spanned;

import com.kt.smartview.R;
import com.kt.smartview.utils.CommonUtil;
import com.kt.smartview.push.PushConstants;
import com.kt.smartview.push.service.MqttAndroidClient;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Connection {

	private String clientHandle = null;
	private String clientId = null;
	private String host = null;
	private int port = 0;
	private ConnectionStatus status = ConnectionStatus.NONE;
	private ArrayList<String> history = null;
	private MqttAndroidClient client = null;
	private ArrayList<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();
	private Context context = null;
	private MqttConnectOptions conOpt;
	private boolean sslConnection = false;
	private long persistenceId = -1;

	public enum ConnectionStatus {
		CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, ERROR, NONE
	}

	public Connection(String clientHandle, String clientId, String host, int port, Context context,
			MqttAndroidClient client, boolean sslConnection) {
		// generate the client handle from its hash code
		this.clientHandle = clientHandle;
		this.clientId = clientId;
		this.host = host;
		this.port = port;
		this.context = context;
		this.client = client;
		this.sslConnection = sslConnection;
		history = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		sb.append("Client: ");
		sb.append(clientId);
		sb.append(" created");
		addAction(sb.toString());
	}

	public void addAction(String action) {

		Object[] args = new String[1];
		SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.dateFormat));
		args[0] = sdf.format(new Date());

		String timestamp = context.getString(R.string.timestamp, args);
		history.add(action + timestamp);

		notifyListeners(new PropertyChangeEvent(this, PushConstants.historyProperty, null, null));
	}

	public Spanned[] history() {

		int i = 0;
		Spanned[] array = new Spanned[history.size()];

		for (String s : history) {
			if (s != null) {
				array[i] = CommonUtil.getHtmlString(s);
				i++;
			}
		}

		return array;

	}

	public String handle() {
		return clientHandle;
	}

	public boolean isConnected() {
		return status == ConnectionStatus.CONNECTED;
	}

	public void changeConnectionStatus(ConnectionStatus connectionStatus) {
		status = connectionStatus;
		notifyListeners((new PropertyChangeEvent(this, PushConstants.ConnectionStatusProperty, null, null)));
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(clientId);
		sb.append("\n ");
		switch (status) {

		case CONNECTED:
			sb.append(context.getString(R.string.connectedto));
			break;
		case DISCONNECTED:
			sb.append(context.getString(R.string.disconnected));
			break;
		case NONE:
			sb.append(context.getString(R.string.no_status));
			break;
		case CONNECTING:
			sb.append(context.getString(R.string.connecting));
			break;
		case DISCONNECTING:
			sb.append(context.getString(R.string.disconnecting));
			break;
		case ERROR:
			sb.append(context.getString(R.string.connectionError));
		}
		sb.append(" ");
		sb.append(host);

		return sb.toString();
	}

	public boolean isHandle(String handle) {
		return clientHandle.equals(handle);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Connection)) {
			return false;
		}

		Connection c = (Connection) o;

		return clientHandle.equals(c.clientHandle);

	}

	public String getId() {
		return clientId;
	}

	public String getHostName() {

		return host;
	}

	public boolean isConnectedOrConnecting() {
		return (status == ConnectionStatus.CONNECTED) || (status == ConnectionStatus.CONNECTING);
	}

	public boolean isConnecting() {
		return (status == ConnectionStatus.CONNECTING);
	}

	public boolean noError() {
		return status != ConnectionStatus.ERROR;
	}

	public MqttAndroidClient getClient() {
		return client;
	}

	public void addConnectionOptions(MqttConnectOptions connectOptions) {
		conOpt = connectOptions;

	}

	public MqttConnectOptions getConnectionOptions() {
		return conOpt;
	}

	public void registerChangeListener(PropertyChangeListener listener) {
		listeners.add(listener);
	}

	public void removeChangeListener(PropertyChangeListener listener) {
		if (listener != null) {
			listeners.remove(listener);
		}
	}

	private void notifyListeners(PropertyChangeEvent propertyChangeEvent) {
		for (PropertyChangeListener listener : listeners) {
			listener.propertyChange(propertyChangeEvent);
		}
	}

	public int getPort() {
		return port;
	}

	public int isSSL() {
		return sslConnection ? 1 : 0;
	}

	public void assignPersistenceId(long id) {
		persistenceId = id;
	}

	public long persistenceId() {
		return persistenceId;
	}

	public ConnectionStatus getConnectionStatus(){
		return status;
	}
}
