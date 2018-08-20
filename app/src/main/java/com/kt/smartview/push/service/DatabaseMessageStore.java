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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.kt.smartview.push.client.MqttTraceHandler;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Iterator;

class DatabaseMessageStore implements MessageStore {

	private static String TAG = "DatabaseMessageStore";

	private static final String MTIMESTAMP = "mtimestamp";
	private static final String ARRIVED_MESSAGE_TABLE_NAME = "MqttArrivedMessageTable";
	private SQLiteDatabase db = null;
	private MQTTDatabaseHelper mqttDb = null;
	private MqttTraceHandler traceHandler = null;

	private static class MQTTDatabaseHelper extends SQLiteOpenHelper {
		private static String TAG = "MQTTDatabaseHelper";
		private static final String DATABASE_NAME = "mqttAndroidService.db";
		private static final int DATABASE_VERSION = 1;
		private MqttTraceHandler traceHandler = null;

		public MQTTDatabaseHelper(MqttTraceHandler traceHandler, Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			this.traceHandler = traceHandler;
		}

		@Override
		public void onCreate(SQLiteDatabase database) {
			String createArrivedTableStatement = "CREATE TABLE " + ARRIVED_MESSAGE_TABLE_NAME + "("
					+ MqttServiceConstants.MESSAGE_ID + " TEXT PRIMARY KEY, " + MqttServiceConstants.CLIENT_HANDLE
					+ " TEXT, " + MqttServiceConstants.DESTINATION_NAME + " TEXT, " + MqttServiceConstants.PAYLOAD
					+ " BLOB, " + MqttServiceConstants.QOS + " INTEGER, " + MqttServiceConstants.RETAINED + " TEXT, "
					+ MqttServiceConstants.DUPLICATE + " TEXT, " + MTIMESTAMP + " INTEGER" + ");";
			traceHandler.traceDebug(TAG, "onCreate {" + createArrivedTableStatement + "}");
			try {
				database.execSQL(createArrivedTableStatement);
				traceHandler.traceDebug(TAG, "created the table");
			} catch (SQLException e) {
				traceHandler.traceException(TAG, "onCreate", e);
				throw e;
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			traceHandler.traceDebug(TAG, "onUpgrade");
			try {
				db.execSQL("DROP TABLE IF EXISTS " + ARRIVED_MESSAGE_TABLE_NAME);
			} catch (SQLException e) {
				traceHandler.traceException(TAG, "onUpgrade", e);
				throw e;
			}
			onCreate(db);
			traceHandler.traceDebug(TAG, "onUpgrade complete");
		}
	}

	public DatabaseMessageStore(MqttService service, Context context) {
		this.traceHandler = service;
		mqttDb = new MQTTDatabaseHelper(traceHandler, context);
		traceHandler.traceDebug(TAG, "DatabaseMessageStore<init> complete");
	}

	@Override
	public String storeArrived(String clientHandle, String topic, MqttMessage message) {

		db = mqttDb.getWritableDatabase();

		traceHandler.traceDebug(TAG, "storeArrived{" + clientHandle + "}, {" + message.toString() + "}");

		byte[] payload = message.getPayload();
		int qos = message.getQos();
		boolean retained = message.isRetained();
		boolean duplicate = message.isDuplicate();

		ContentValues values = new ContentValues();
		String id = java.util.UUID.randomUUID().toString();
		values.put(MqttServiceConstants.MESSAGE_ID, id);
		values.put(MqttServiceConstants.CLIENT_HANDLE, clientHandle);
		values.put(MqttServiceConstants.DESTINATION_NAME, topic);
		values.put(MqttServiceConstants.PAYLOAD, payload);
		values.put(MqttServiceConstants.QOS, qos);
		values.put(MqttServiceConstants.RETAINED, retained);
		values.put(MqttServiceConstants.DUPLICATE, duplicate);
		values.put(MTIMESTAMP, System.currentTimeMillis());
		try {
			db.insertOrThrow(ARRIVED_MESSAGE_TABLE_NAME, null, values);
		} catch (SQLException e) {
			traceHandler.traceException(TAG, "onUpgrade", e);
			throw e;
		}
		int count = getArrivedRowCount(clientHandle);
		traceHandler.traceDebug(TAG, "storeArrived: inserted message with id of {" + id
				+ "} - Number of messages in database for this clientHandle = " + count);
		return id;
	}

	private int getArrivedRowCount(String clientHandle) {
		String[] cols = new String[1];
		cols[0] = "COUNT(*)";
		Cursor c = db.query(ARRIVED_MESSAGE_TABLE_NAME, cols,
				MqttServiceConstants.CLIENT_HANDLE + "='" + clientHandle + "'", null, null, null, null);
		int count = 0;
		if (c != null) {
			if(c.moveToFirst()){
				count = c.getInt(0);
			}
			c.close();
		}
		return count;
	}

	@Override
	public boolean discardArrived(String clientHandle, String id) {

		db = mqttDb.getWritableDatabase();

		traceHandler.traceDebug(TAG, "discardArrived{" + clientHandle + "}, {" + id + "}");
		int rows;
		try {
			rows = db.delete(ARRIVED_MESSAGE_TABLE_NAME, MqttServiceConstants.MESSAGE_ID + "='" + id + "' AND "
					+ MqttServiceConstants.CLIENT_HANDLE + "='" + clientHandle + "'", null);
		} catch (SQLException e) {
			traceHandler.traceException(TAG, "discardArrived", e);
			throw e;
		}
		if (rows != 1) {
			traceHandler.traceError(TAG,
					"discardArrived - Error deleting message {" + id + "} from database: Rows affected = " + rows);
			return false;
		}
		int count = getArrivedRowCount(clientHandle);
		traceHandler.traceDebug(TAG,
				"discardArrived - Message deleted successfully. - messages in db for this clientHandle " + count);
		return true;
	}

	@Override
	public Iterator<StoredMessage> getAllArrivedMessages(final String clientHandle) {
		return new Iterator<StoredMessage>() {
			private Cursor c;
			private boolean hasNext;

			{
				db = mqttDb.getWritableDatabase();
				if (clientHandle == null) {
					c = db.query(ARRIVED_MESSAGE_TABLE_NAME, null, null, null, null, null, "mtimestamp ASC");
				} else {
					c = db.query(ARRIVED_MESSAGE_TABLE_NAME, null,
							MqttServiceConstants.CLIENT_HANDLE + "='" + clientHandle + "'", null, null, null,
							"mtimestamp ASC");
				}
				hasNext = c.moveToFirst();
			}

			@Override
			public boolean hasNext() {
				if (hasNext == false) {
					c.close();
				}
				return hasNext;
			}

			@Override
			public StoredMessage next() {
				String messageId = c.getString(c.getColumnIndex(MqttServiceConstants.MESSAGE_ID));
				String clientHandle = c.getString(c.getColumnIndex(MqttServiceConstants.CLIENT_HANDLE));
				String topic = c.getString(c.getColumnIndex(MqttServiceConstants.DESTINATION_NAME));
				byte[] payload = c.getBlob(c.getColumnIndex(MqttServiceConstants.PAYLOAD));
				int qos = c.getInt(c.getColumnIndex(MqttServiceConstants.QOS));
				boolean retained = Boolean.parseBoolean(c.getString(c.getColumnIndex(MqttServiceConstants.RETAINED)));
				boolean dup = Boolean.parseBoolean(c.getString(c.getColumnIndex(MqttServiceConstants.DUPLICATE)));

				// 결과값 만들기
				MqttMessageHack message = new MqttMessageHack(payload);
				message.setQos(qos);
				message.setRetained(retained);
				message.setDuplicate(dup);

				// 커서 이동
				hasNext = c.moveToNext();
				return new DbStoredData(messageId, clientHandle, topic, message);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			protected void finalize() throws Throwable {
				c.close();
				super.finalize();
			}

		};
	}

	@Override
	public void clearArrivedMessages(String clientHandle) {

		db = mqttDb.getWritableDatabase();

		int rows = 0;
		if (clientHandle == null) {
			traceHandler.traceDebug(TAG, "clearArrivedMessages: clearing the table");
			rows = db.delete(ARRIVED_MESSAGE_TABLE_NAME, null, null);
		} else {
			traceHandler.traceDebug(TAG, "clearArrivedMessages: clearing the table of " + clientHandle + " messages");
			rows = db.delete(ARRIVED_MESSAGE_TABLE_NAME, MqttServiceConstants.CLIENT_HANDLE + "='" + clientHandle + "'",
					null);
		}
		traceHandler.traceDebug(TAG, "clearArrivedMessages: rows affected = " + rows);
		return;
	}

	private class DbStoredData implements StoredMessage {
		private String messageId;
		private String clientHandle;
		private String topic;
		private MqttMessage message;

		DbStoredData(String messageId, String clientHandle, String topic, MqttMessage message) {
			this.messageId = messageId;
			this.topic = topic;
			this.message = message;
		}

		@Override
		public String getMessageId() {
			return messageId;
		}

		@Override
		public String getClientHandle() {
			return clientHandle;
		}

		@Override
		public String getTopic() {
			return topic;
		}

		@Override
		public MqttMessage getMessage() {
			return message;
		}
	}

	private class MqttMessageHack extends MqttMessage {

		public MqttMessageHack(byte[] payload) {
			super(payload);
		}

		@Override
		protected void setDuplicate(boolean dup) {
			super.setDuplicate(dup);
		}
	}

	@Override
	public void close() {
		if (this.db != null)
			this.db.close();

	}

}