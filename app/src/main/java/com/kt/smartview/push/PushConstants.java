package com.kt.smartview.push;

import com.kt.smartview.common.Constants;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class PushConstants {

	/* Default values **/
	public static final String defaultServerIp = Constants.PUSH_DOMAIN_URL;
	public static final int defaultPort = 61060;
	public static final int defaultTimeOut = 5000;
	public static final int defaultKeepAlive = 30;
	public static final boolean defaultSsl = false;
	public static final boolean defaultRetained = false;
	public static final MqttMessage defaultLastWill = null;
	
	/** Request Code **/
	public static final int connect = 0;
	public static final int advancedConnect = 1;
	public static final int lastWill = 2;
	public static final int showHistory = 3;


	public static final String historyProperty = "history";
	public static final String ConnectionStatusProperty = "connectionStatus";
	public static final String space = " ";
	public static final String empty = new String();

	public static final String TOPIC_PREFIX = "smartview/noti";
	public static final String lastWillTopic = TOPIC_PREFIX;

}
