package com.kt.smartview.push;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kt.smartview.common.Constants;
import com.kt.smartview.support.log.YWMLog;
import com.kt.smartview.support.preference.SmartViewPreference;
import com.kt.smartview.utils.SSLUtil;
import com.kt.smartview.vo.AlarmSettingVo;

import java.lang.reflect.Type;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class DomaMqttClient extends PushClient {
    private final YWMLog logger = new YWMLog(this.getClass());
    private Context context;
    public DomaMqttClient(Context context, String clientId){
        super(clientId);
        this.context = context;
        setSsl(true);
    }

    @Override
    public void createTopics() {
        String[] topicArray = makeTopics();
        int[] qosArray = new int[topicArray.length];
        int idx = 0;
        for(String topic : topicArray){
            logger.d(String.format("topic[%d] : %s", idx, topic));
            qosArray[idx] = QOS_2;
            idx++;
        }
        setQos(qosArray);
        setTopics(topicArray);
    }

    @Override
    public String[] makeTopics() {
        String[] topicArray = null;
        int cnt = 0;
        String json = SmartViewPreference.getSettingText(context, null);
        if(json != null){
            Gson gson = new Gson();
            Type type = new TypeToken<List<AlarmSettingVo>>(){}.getType();
            List<AlarmSettingVo> list = gson.fromJson(json, type);
            if(list != null){
                topicArray = new String[list.size()];
                for(AlarmSettingVo vo : list){
                    String topic = String.format("%s/%s/#", PushConstants.TOPIC_PREFIX, vo.getAlarmCode());
                    topicArray[cnt++] = topic;
                }
            }
        }
        return topicArray;
    }

    @Override
    public SSLSocketFactory getSocketFactory() {
        SSLSocketFactory sslSocketFactory = null;
        try{
            sslSocketFactory = SSLUtil.getSocketFactory(context, Constants.SSL_CLIENT_CA_PATH, Constants.SSL_CLIENT_CRT_PATH, Constants.SSL_SERVER_KEY_PATH, Constants.SSL_CLIENT_CERT_PWD);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
        return sslSocketFactory;
    }
}