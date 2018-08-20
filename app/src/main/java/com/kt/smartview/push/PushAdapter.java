package com.kt.smartview.push;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.kt.smartview.support.log.YWMLog;
import com.kt.smartview.push.client.Connection;
import com.kt.smartview.push.client.MqttTraceHandler;
import com.kt.smartview.push.service.MqttAndroidClient;
import com.kt.smartview.push.service.MqttService;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.util.Arrays;

import javax.net.ssl.SSLSocketFactory;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class PushAdapter {
    private final YWMLog logger = new YWMLog(this.getClass());
    private final String TAG = "PUSH_TEST";
    private Context context;
    private PushClient pushClient;
    private String serverUri;
    private String clientHandle;
    private IMqttToken subscribeToken;
    private PushCallbackListener pushCallbackListener;
    private Connection connection;
    private MqttService service;
    enum Action {
        CONNECT, DISCONNECT, SUBSCRIBE, PUBLISH
    }
    public PushAdapter(Context context, PushClient pushClient, PushCallbackListener pushCallbackListener, MqttService service){
        this.context = context;
        this.pushClient = pushClient;
        this.pushCallbackListener = pushCallbackListener;
        this.service = service;
        clientHandle = createHandle(serverUri, pushClient.getClientId());
    }

    public static String createHandle(String serverUri, String clientId){
        return serverUri + clientId;
    }

    private boolean retryConnect(){
        if(connection != null && connection.isConnectedOrConnecting() == false && connection.getConnectionOptions() != null){
            MqttAndroidClient client = connection.getClient();
            if(client != null && client.isConnected() != false){
                try {
                    IMqttToken token = client.connect(connection.getConnectionOptions(), context,
                            new ActionListener(Action.CONNECT));
                    return true;
                } catch (MqttSecurityException e) {
                    Log.e(this.getClass().getCanonicalName(), "Failed to reconnect the client with the handle " + clientHandle,
                            e);
                    connection.addAction("Client failed to connect");
                    return false;
                } catch (MqttException e) {
                    Log.e(this.getClass().getCanonicalName(), "Failed to reconnect the client with the handle " + clientHandle,
                            e);
                    connection.addAction("Client failed to connect");
                    return false;
                } catch (Exception e) {
                    Log.e(this.getClass().getCanonicalName(), "Failed to reconnect the client with the handle " + clientHandle,
                            e);
                    connection.addAction("Client failed to connect");
                    return false;
                }
            }
        }
        return false;
    }
    private void initialize(){
        subscribeToken = null;
        String uri = null;
        if (pushClient.isSsl()) {
            Log.e("SSLConnection", "Doing an SSL Connect");
            uri = "ssl://";

        } else {
            uri = "tcp://";
        }
        serverUri = uri + pushClient.getServerIp() + ":" + pushClient.getPort();
    }
    private MqttConnectOptions getConnectOptions(MqttAndroidClient client){
        MqttConnectOptions conOpt = new MqttConnectOptions();
        conOpt.setCleanSession(pushClient.isCleanSession());
        if (pushClient.isSsl()) {
            SSLSocketFactory sslSocketFactory = pushClient.getSocketFactory();
            if(sslSocketFactory != null){
                conOpt.setSocketFactory(sslSocketFactory);
            }
        }

        String topic = PushConstants.lastWillTopic;
        Integer qos = PushClient.QOS_2;
        Boolean retained = pushClient.isRetained();
        String message = String.format("Tring to send for LastWill");
        conOpt.setConnectionTimeout(pushClient.getTimeOut());
        conOpt.setKeepAliveInterval(pushClient.getKeepAlive());
		if (TextUtils.isEmpty(pushClient.getUserName()) == false) {
			conOpt.setUserName(pushClient.getUserName());
		}
        if (TextUtils.isEmpty(pushClient.getPassword()) == false) {
			conOpt.setPassword(pushClient.getPassword().toCharArray());
		}

        conOpt.setWill(topic, message.getBytes(), qos.intValue(), retained);
        return conOpt;
    }

    private void doConnect(){
        initialize();
        MqttAndroidClient client = new MqttAndroidClient(context, service, serverUri, pushClient.getClientId());
        connection = new Connection(clientHandle, pushClient.getClientId(), pushClient.getServerIp(), pushClient.getPort(), context, client, pushClient.isSsl());
        String[] actionArgs = new String[1];
        actionArgs[0] = pushClient.getClientId();
        connection.changeConnectionStatus(Connection.ConnectionStatus.CONNECTING);
        final ActionListener callback = new ActionListener(Action.CONNECT);
        MqttConnectOptions conOpt = getConnectOptions(client);

        MqttCallbackHandler mqttCallback = new MqttCallbackHandler(context, clientHandle);
        client.setCallback(mqttCallback);
        client.setTraceCallback(new MqttTraceCallback());
        client.setTraceEnabled(true);
        connection.addConnectionOptions(conOpt);
        try {
            client.connect(conOpt, null, callback);
        } catch (MqttException e) {
            Log.e(context.getClass().getCanonicalName(), "MqttException Occured", e);
        }
    }
    public void connect() {
        if(retryConnect() == false){
            doConnect();
        }
    }

    private boolean reconnect() {
        if(isConnected())  {
            return false;
        }
        if(connection != null && connection.isConnectedOrConnecting()== false && connection.getConnectionOptions() != null){
            MqttAndroidClient client = connection.getClient();
            if(client != null){
                client.reconnect();
                return true;
            }
        }
        return false;
    }
    private ActionListener getActionListener(String id, Action action){
        String[] actionArgs = new String[1];
        actionArgs[0] = id;
        final ActionListener callback = new ActionListener(action);
        return callback;
    };


    private class ActionListener implements IMqttActionListener {

        private Action action;

        public ActionListener(Action action) {
            this.action = action;
        }

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            switch (action) {
                case CONNECT:
                    Log.e(TAG, "CONNECT...");
                    if(service != null){
                        service.cancelReconnect();
                    }
                    subscribe();
                    break;
                case DISCONNECT:
                    Log.e(TAG, "DISCONNECT...");
                    break;
                case SUBSCRIBE:
                    Log.e(TAG, "SUBSCRIBE...");
                    subscribeToken = asyncActionToken;
                    break;
                case PUBLISH:
                    Log.e(TAG, "PUBLISH...");
                    break;
            }

        }



        @Override
        public void onFailure(IMqttToken token, Throwable exception) {
            switch (action) {
                case CONNECT:
                    Log.e(TAG, "CONNECT onFailure..." + exception.getMessage());
                    if(service != null){
                        service.scheduleReconnect();
                    }
                    break;
                case DISCONNECT:
                    Log.e(TAG, "DISCONNECT onFailure..." + exception.getMessage());
                    break;
                case SUBSCRIBE:
                    Log.e(TAG, "SUBSCRIBE onFailure..." + exception.getMessage());
                    break;
                case PUBLISH:
                    Log.e(TAG, "PUBLISH onFailure..." + exception.getMessage());
                    break;
            }

        }
    }
    public MqttAndroidClient getClient(){
        if(connection != null){
            return connection.getClient();
        }
        return null;
    }

    public void disconnect() {
        if (connection == null || !connection.isConnected()) {
            return;
        }
        MqttAndroidClient client = getClient();
        if(client != null){
            try {

                client.disconnect(null, new ActionListener(Action.DISCONNECT));
                connection.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTING);
            } catch (MqttException e) {
                Log.e(this.getClass().getCanonicalName(), "Failed to disconnect the client with the handle " + clientHandle,
                        e);
                connection.addAction("Client failed to disconnect");
            }
        }
    }

    private void subscribe() {
        pushClient.createTopics();
        String[] topics = pushClient.getTopics();
        subscribeTopcis(topics);
    }

    public void onTopicChanged() {
        String[] topics = null;
        String[] new_topics = pushClient.getTopics();
        if(subscribeToken != null){
            topics = subscribeToken.getTopics();
        }

        boolean isEquals = compareTwoTopic(topics, new_topics);
        if(isEquals == false){
            try {
                reloadSubscribe(topics, new_topics);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }
    private void subscribeTopcis(String[] subscribedTopics){
        if(subscribedTopics == null) return;
        MqttAndroidClient client = getClient();
        if(client != null && client.isConnected()){
//            pushClient.createTopics(subscribedTopics);
            int[] qos = pushClient.getQos();
            try {
                client.subscribe(subscribedTopics, qos, null,
                        new ActionListener(Action.SUBSCRIBE));
            } catch (MqttSecurityException e) {
                Log.e(this.getClass().getCanonicalName(),
                        "Failed to subscribe to (" + Arrays.toString(subscribedTopics) + ") the client with the handle " + clientHandle, e);
            } catch (MqttException e) {
                Log.e(this.getClass().getCanonicalName(),
                        "Failed to subscribe to (" + Arrays.toString(subscribedTopics) + ") the client with the handle " + clientHandle, e);
            }
        }
    }

    public void refreshTopic() throws MqttException{
        boolean isEquals = false;
        String[] topics = null, new_topics = null;
        if(subscribeToken != null){
            topics = subscribeToken.getTopics();
            new_topics = pushClient.getTopics();
            isEquals = compareTwoTopic(topics, new_topics);
        }
        logger.d("refreshTopic isEquals-->" + isEquals);
        if(isEquals == false){
            reloadSubscribe(topics, new_topics);
        }
    }
    private boolean compareTwoTopic(String[] topics, String[] new_topics){
        boolean isEquals = false;
        if(topics != null && new_topics != null){
            String oldStr = "";
            String newStr = "";
            for(String str : topics){
                oldStr = oldStr + str;
            }
            for(String str : new_topics){
                newStr = newStr + str;
            }
            if(oldStr.hashCode() == newStr.hashCode()){
                isEquals = true;
            }
        }else if(topics == null && new_topics == null){
            isEquals = true;
        }
        return isEquals;
    }

    private void reloadSubscribe(String[] topics, final String[] new_topics) throws  MqttException{
        if(isConnected() == false) return;
        MqttAndroidClient client = getClient();
        if(topics != null){
            client.unsubscribe(topics, context, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    logger.d("Topic unsubcribe onSuccess.");
                    subscribeTopcis(new_topics);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    logger.d("Topic unsubcribe failed.");
                }
            });
        }else{
            subscribeTopcis(new_topics);
        }
    }

    public boolean isConnected(){
        MqttAndroidClient client = getClient();
        if(client != null){
            return client.isConnected();
        }
        return false;
    }

    public class MqttCallbackHandler implements MqttCallback {

        private Context context;
        private String clientHandle;

        public MqttCallbackHandler(Context context, String clientHandle) {
            this.context = context;
            this.clientHandle = clientHandle;
        }

        @Override
        public void connectionLost(Throwable cause) {
            if(service != null){
                service.scheduleReconnect();
            }
        }
        @Override
        public void messageArrived(String topic, final MqttMessage message) throws Exception {
            if(message == null || message.getPayload() == null || message.getPayload().length == 0) return;
            String messageString = new String(message.getPayload());
            logger.e(String.format("topic : %s, payload : %s", topic,  messageString));
            pushCallbackListener.messageArrived(message.getPayload());
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // Do nothing
            try {
                logger.d("deliveryComplete-->" + new String(token.getMessage().getPayload()));
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }
    private class MqttTraceCallback implements MqttTraceHandler {

        public void traceDebug(String arg0, String arg1) {
            logger.i(arg0, arg1);
        };

        public void traceError(String arg0, String arg1) {
            logger.d(arg0, arg1);
        };

        public void traceException(String arg0, String arg1, Exception arg2) {
            logger.d(arg0, arg1, arg2);
        };
    }

    public String getClientHandle(){
        return clientHandle;
    }

}
