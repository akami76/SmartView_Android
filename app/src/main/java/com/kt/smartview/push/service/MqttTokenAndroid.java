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

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

class MqttTokenAndroid implements IMqttToken {

	private IMqttActionListener listener;

	private volatile boolean isComplete;

	private volatile MqttException lastException;

	private Object waitObject = new Object();

	private MqttAndroidClient client;

	private Object userContext;

	private String[] topics;

	private IMqttToken delegate; // specifically for getMessageId

	private MqttException pendingException;

	MqttTokenAndroid(MqttAndroidClient client, Object userContext, IMqttActionListener listener) {
		this(client, userContext, listener, null);
	}

	MqttTokenAndroid(MqttAndroidClient client, Object userContext, IMqttActionListener listener, String[] topics) {
		this.client = client;
		this.userContext = userContext;
		this.listener = listener;
		this.topics = topics;
	}

	@Override
	public void waitForCompletion() throws MqttException {
		synchronized (waitObject) {
			try {
				waitObject.wait();
			} catch (InterruptedException e) {
				// do nothing
			}
		}
		if (pendingException != null) {
			throw pendingException;
		}
	}

	@Override
	public void waitForCompletion(long timeout) throws MqttException {
		synchronized (waitObject) {
			try {
				waitObject.wait(timeout);
			} catch (InterruptedException e) {
				// do nothing
			}
			if (!isComplete) {
				throw new MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
			}
			if (pendingException != null) {
				throw pendingException;
			}
		}
	}

	void notifyComplete() {
		synchronized (waitObject) {
			isComplete = true;
			waitObject.notifyAll();
			if (listener != null) {
				listener.onSuccess(this);
			}
		}
	}

	void notifyFailure(Throwable exception) {
		synchronized (waitObject) {
			isComplete = true;
			if (exception instanceof MqttException) {
				pendingException = (MqttException) exception;
			} else {
				pendingException = new MqttException(exception);
			}
			waitObject.notifyAll();
			if (exception instanceof MqttException) {
				lastException = (MqttException) exception;
			}
			if (listener != null) {
				listener.onFailure(this, exception);
			}
		}

	}

	@Override
	public boolean isComplete() {
		return isComplete;
	}

	void setComplete(boolean complete) {
		isComplete = complete;
	}

	@Override
	public MqttException getException() {
		return lastException;
	}

	void setException(MqttException exception) {
		lastException = exception;
	}

	@Override
	public IMqttAsyncClient getClient() {
		return client;
	}

	@Override
	public void setActionCallback(IMqttActionListener listener) {
		this.listener = listener;
	}

	@Override
	public IMqttActionListener getActionCallback() {
		return listener;
	}

	@Override
	public String[] getTopics() {
		return topics;
	}

	@Override
	public void setUserContext(Object userContext) {
		this.userContext = userContext;

	}

	@Override
	public Object getUserContext() {
		return userContext;
	}

	void setDelegate(IMqttToken delegate) {
		this.delegate = delegate;
	}

	@Override
	public int getMessageId() {
		return (delegate != null) ? delegate.getMessageId() : 0;
	}

	@Override
	public MqttWireMessage getResponse() {
		return delegate.getResponse();
	}

	@Override
	public boolean getSessionPresent() {
		return delegate.getSessionPresent();
	}

	@Override
	public int[] getGrantedQos() {
		return delegate.getGrantedQos();
	}

}
