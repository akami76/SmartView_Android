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

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Iterator;

interface MessageStore {

	interface StoredMessage {
		String getMessageId();

		String getClientHandle();

		String getTopic();

		MqttMessage getMessage();
	}

	String storeArrived(String clientHandle, String Topic, MqttMessage message);

	boolean discardArrived(String clientHandle, String id);

	Iterator<StoredMessage> getAllArrivedMessages(String clientHandle);

	void clearArrivedMessages(String clientHandle);

	void close();
}
