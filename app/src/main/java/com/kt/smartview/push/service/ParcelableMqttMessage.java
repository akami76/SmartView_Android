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

import android.os.Parcel;
import android.os.Parcelable;

import org.eclipse.paho.client.mqttv3.MqttMessage;

class ParcelableMqttMessage extends MqttMessage implements Parcelable {

	String messageId = null;

	ParcelableMqttMessage(MqttMessage original) {
		super(original.getPayload());
		setQos(original.getQos());
		setRetained(original.isRetained());
		setDuplicate(original.isDuplicate());
	}

	ParcelableMqttMessage(Parcel parcel) {
		super(parcel.createByteArray());
		setQos(parcel.readInt());
		boolean[] flags = parcel.createBooleanArray();
		setRetained(flags[0]);
		setDuplicate(flags[1]);
		messageId = parcel.readString();
	}

	public String getMessageId() {
		return messageId;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeByteArray(getPayload());
		parcel.writeInt(getQos());
		parcel.writeBooleanArray(new boolean[] { isRetained(), isDuplicate() });
		parcel.writeString(messageId);
	}

	public static final Creator<ParcelableMqttMessage> CREATOR = new Creator<ParcelableMqttMessage>() {

		@Override
		public ParcelableMqttMessage createFromParcel(Parcel parcel) {
			return new ParcelableMqttMessage(parcel);
		}

		@Override
		public ParcelableMqttMessage[] newArray(int size) {
			return new ParcelableMqttMessage[size];
		}
	};
}