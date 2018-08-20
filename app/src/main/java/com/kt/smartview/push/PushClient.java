package com.kt.smartview.push;

import java.text.Collator;
import java.util.Comparator;

import javax.net.ssl.SSLSocketFactory;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public abstract class PushClient {

    private String serverIp;
    private int port;
    private String clientId;
    private boolean isRetained;
    private int[] qos = null;
    private String[] topics = null;
    private int keepAlive = 60;
    private int timeOut = 30;

    private boolean cleanSession;
    public static final int QOS_0 = 0;
    public static final int QOS_1 = 1;
    public static final int QOS_2 = 2;

    private String userName= null;
    private String password = null;

    private boolean ssl;
    private String ssl_key;

    public final static Comparator<String> roomIdomparator= new Comparator<String>() {
        private final Collator collator = Collator.getInstance();

        @Override
        public int compare(String object1,String object2) {
            return (Double.parseDouble (object1) > Double.parseDouble (object2) ? 1: -1);
        }
    };
    public abstract void createTopics();
    public abstract String[] makeTopics();
    public abstract SSLSocketFactory getSocketFactory();
    public PushClient(String clientId) {
        this.clientId = null;
        this.ssl = PushConstants.defaultSsl;
        this.serverIp = PushConstants.defaultServerIp;
        this.port = PushConstants.defaultPort;
        this.isRetained = false;
        this.qos = null;
        this.topics = null;
        this.timeOut = PushConstants.defaultTimeOut;
        this.keepAlive = PushConstants.defaultKeepAlive;
        this.cleanSession = false;
        this.clientId = clientId;
        this.ssl_key = null;
    }
    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public boolean isRetained() {
        return isRetained;
    }

    public void setRetained(boolean retained) {
        isRetained = retained;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public int getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public String[] getTopics() {
        return topics;
    }

    public void setTopics(String[] topics) {
        this.topics = topics;
    }

    public int[] getQos() {
        return qos;
    }

    public void setQos(int[] qos) {
        this.qos = qos;
    }


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSsl_key() {
        return ssl_key;
    }

    public void setSsl_key(String ssl_key) {
        this.ssl_key = ssl_key;
    }
}
