package com.kt.smartview.push;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.kt.smartview.push.service.MqttAndroidClient;
import com.kt.smartview.push.service.MqttService;
import com.kt.smartview.support.log.YWMLog;
import com.kt.smartview.support.preference.SmartViewPreference;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Date;
import java.util.concurrent.Executor;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class PushService extends MqttService implements PushCallbackListener{
    private final YWMLog logger = new YWMLog(this.getClass());
    private final String TAG = "PushManager";
    public static final String ACTION = "PushService";
    private Context context;
    private PushAdapter pushAdapter;
    public static final String ACTION_TOPIC_RELOAD = "ACTION_TOPIC_RELOAD";
    public static final String ACTION_TOPIC_ADDED = "ACTION_TOPIC_ADDED";
    public static final String ACTION_TOPIC_DELETED = "ACTION_TOPIC_DELETED";
    public static final String ACTION_CHECK_CONNECTION = "ACTION_CHECK_CONNECTION";

    public static final int PUSH_SEND_CHAT_READ_OK = 0x01;
    public static final int PUSH_SEND_RESULT = 0x02;
    public static final String EXTRA_TOPIC_NAME = "EXTRA_TOPIC_NAME";
    public static final String EXTRA_TOPIC_LIST = "EXTRA_TOPIC_LIST";
    private final int ALIVE_INTERVAL = 30 * 1000;
    private Gson gson;
    private static final long		INITIAL_RETRY_INTERVAL = 1000 * 10;
    private static final long		MAXIMUM_RETRY_INTERVAL = 1000 * 60 * 3;
    private static final String MQTT_RECONNECT_ACTION = "com.kt.smartview.RECONNECT";
    private ReconnectReceiver reconnectReceiver;
    private PendingIntent reconnectPendingIntent;
    private long alarmInterval = 0L;
    //private ExecutorService pool = Executors.newCachedThreadPool();
    private PushHandler pushHandler;

    @Override
    public MqttAndroidClient getMqttAndroidClient() {
        if(pushAdapter != null){
            return pushAdapter.getClient();
        }
        return null;
    }

    @Override
    public PushAdapter getPushAdapter() {
        return pushAdapter;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        gson = new Gson();
        context = getApplicationContext();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);
        if(SmartViewPreference.isPushOn(getApplicationContext()) == false){
            return START_STICKY;
        }
        if(intent != null){
            logger.d("intent.getAction()-->" + intent.getAction());
            if(ACTION_TOPIC_RELOAD.equals(intent.getAction())){
                try {
                    refreshTopic();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }else if(ACTION_TOPIC_ADDED.equals(intent.getAction())){
                try {
                    String topic = intent.getStringExtra(EXTRA_TOPIC_NAME);
                    onSubscribeInserted(topic);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }else if(ACTION_TOPIC_DELETED.equals(intent.getAction())){
                try {
                    String topic = intent.getStringExtra(EXTRA_TOPIC_NAME);
                    onSubscribeDeleted(topic);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }else{
                initialize();
            }
        }
        return START_STICKY;
    }

    private void initialize(){
        if(pushHandler == null){
            pushHandler = new PushHandler(getApplicationContext());
        }
        unregisterRestartAlarm();
        regReceiver();
        connectToBroker();
    }

    private void connectToBroker(){
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PushService");
        try{
            wl.acquire();
            if(pushAdapter == null){
                pushAdapter = new PushAdapter(context, new DomaMqttClient(context, generateClientId(context)), this, getMqttService());
            }
            if(pushAdapter.isConnected() == false) {
                pushAdapter.connect();
            }
        } catch(Exception e){
            e.printStackTrace();
        }finally {
            wl.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logger.d("PushService onDestory...");
        if(pushAdapter != null){
            pushAdapter.disconnect();
        }
        clearReceiver();
        if(SmartViewPreference.isPushOn(getApplicationContext())){
            registerRestartAlarm();
        }
    }

    private void registerRestartAlarm() {
        unregisterRestartAlarm();
        Intent intent = new Intent(getApplicationContext(), RestartService.class);
        PendingIntent sender = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
        long firstTime = SystemClock.elapsedRealtime();
        firstTime += ALIVE_INTERVAL; // 10초 후에 알람이벤트 발생
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, ALIVE_INTERVAL, sender);
    }

    private void unregisterRestartAlarm() {
        // Log.d(Constants.TAG, "unregisterRestartAlarm");
        Intent intent = new Intent(getApplicationContext(), RestartService.class);
        PendingIntent sender = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.cancel(sender);
    }

    public void scheduleReconnect() {
        cancelReconnect();
        if(SmartViewPreference.isPushOn(context)){
            long now = System.currentTimeMillis();
            alarmInterval = Math.min((alarmInterval + INITIAL_RETRY_INTERVAL), MAXIMUM_RETRY_INTERVAL);
            logger.d("Rescheduling connection in " + alarmInterval + "ms.");
            reconnectPendingIntent = PendingIntent
                    .getBroadcast(this, 0, new Intent(MQTT_RECONNECT_ACTION), PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmMgr.set(AlarmManager.RTC_WAKEUP, now + alarmInterval, reconnectPendingIntent);
        }
    }

    public void cancelReconnect() {
        if(reconnectPendingIntent != null){
            logger.d("cancelReconnect Alarm...");
            AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmMgr.cancel(reconnectPendingIntent);
            alarmInterval = 0L;
        }
    }


    private void regReceiver(){
        if (reconnectReceiver == null) {
            reconnectReceiver = new ReconnectReceiver();
            registerReceiver(reconnectReceiver, new IntentFilter(MQTT_RECONNECT_ACTION));
        }
    }
    private void clearReceiver(){
        if (reconnectReceiver != null) {
            unregisterReceiver(reconnectReceiver);
        }
    }
    public class ReconnectReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            logger.d("ReconnectReceiver onReceive...");
            if(SmartViewPreference.isPushOn(getApplicationContext()) == false){
                return;
            }
            if(isOnline()){
                if(pushAdapter != null && pushAdapter.isConnected()){
                    cancelReconnect();
                }else{
                    connectToBroker();
                }
            }else{
                scheduleReconnect();
            }
        }
    }

    @Override
    public void messageArrived(final byte[] payload) {
        if(SmartViewPreference.isLogin(getApplicationContext())){
            createTaskExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    pushHandler.messageArrived(payload);
                }
            });
        }
    }

    @Override
    public MqttService getMqttService() {
        return this;
    }

    private boolean isIdValidate(String id){
        if(TextUtils.isEmpty(id) == false && id.indexOf("/") > 1){
            String[] strArray = id.split("/");
            for(String str : strArray){
                str.trim();
                if(TextUtils.isEmpty(str)){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private String generateClientId(Context context) {
        /*
        * TODO
        * userId + ANDROID_ID 조합으로 사용해야함.
         */
        String userId = SmartViewPreference.getUserId(context);
        String clientId = Settings.System.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        if (clientId == null) {
            String timestamp = "" + (new Date()).getTime();
            clientId = timestamp;
        }
        return String.format("smartview_%s", userId); //clientId,
    }

    private void refreshTopic() throws MqttException {
        if(pushAdapter != null){
            if(pushAdapter.isConnected()){
                pushAdapter.refreshTopic();
            }else{
                pushAdapter.connect();
            }
        }
    }

    private void onSubscribeDeleted(String roomId) throws MqttException {
        logger.d("onSubscribeDeleted roomId-->" + roomId);
        if(pushAdapter != null){
            /*if(pushAdapter.isTopicSubscribed(roomId)){
                if(pushAdapter.isConnected()){
                    pushAdapter.unsubcribe(roomId);
                }else{
                    pushAdapter.connect();
                }
            }*/
            if(pushAdapter.isConnected()){
                pushAdapter.onTopicChanged();
            }else{
                pushAdapter.connect();
            }
        }
    }
    private void onSubscribeInserted(String roomId) throws MqttException {
        logger.d("onSubscribeInserted roomId-->" + roomId);
        if(pushAdapter != null){
            /*if(pushAdapter.isTopicSubscribed(roomId) == false){
                if(pushAdapter.isConnected()){
                    pushAdapter.subcribe(roomId);
                }else{
                    pushAdapter.connect();
                }
            }*/
            if(pushAdapter.isConnected()){
                pushAdapter.onTopicChanged();
            }else{
                pushAdapter.connect();
            }
        }
    }
    private Executor createTaskExecutor() {
        return PushThreadFactory
                .createExecutor(PushThreadFactory.DEFAULT_THREAD_POOL_SIZE, PushThreadFactory.DEFAULT_THREAD_PRIORITY,
                        PushThreadFactory.DEFAULT_TASK_PROCESSING_TYPE);
    }
    @Override
    public String getClientHandle() {
        if(pushAdapter != null){
            pushAdapter.getClientHandle();
        }
        return null;
    }
}
