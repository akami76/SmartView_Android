package com.kt.smartview.push;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kt.smartview.R;
import com.kt.smartview.common.Constants;
import com.kt.smartview.db.DomaDBHandler;
import com.kt.smartview.support.log.YWMLog;
import com.kt.smartview.support.notification.NotificationCenter;
import com.kt.smartview.support.preference.SettingPreference;
import com.kt.smartview.support.preference.SmartViewPreference;
import com.kt.smartview.ui.activity.AlarmHistoryActivity;
import com.kt.smartview.utils.CommonUtil;
import com.kt.smartview.vo.AlarmSettingVo;
import com.kt.smartview.vo.AlarmVo;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */

public class PushHandler {
    private final YWMLog logger = new YWMLog(PushHandler.class);
    private Context context;
    private DomaDBHandler dbHandler;
    private Gson gson = null;
    private NotificationCenter notificationCenter;

    public PushHandler(Context context){
        this.context = context;
        notificationCenter = new NotificationCenter(context);
        dbHandler = new DomaDBHandler(context);
        gson = new Gson();
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

    public void messageArrived(byte[] payload){
        if(payload == null) return;
        try{
            String strPayload = new String(payload, "UTF-8");
            AlarmVo alarmVo = gson.fromJson(strPayload, AlarmVo.class);
            if(alarmVo != null){
                String alarmTitle = getAlarmTitle(alarmVo.getAlarmCode());
                alarmVo.setTitle(alarmTitle);
                alarmVo.setIsReadOk("N");
                alarmVo.setIsClickOk("N");
                long result = dbHandler.insertAlarmHistory(alarmVo);
                if(result > 0){
                    logger.i(String.format("insertAlarmHistory [%d]", result));
                    logger.i(String.format("getAlarmCode = %s", alarmVo.getAlarmCode()));
                    boolean isEnableAlarmNoti = SettingPreference.getBoolean(context, alarmVo.getAlarmCode(), true);
                    logger.i(String.format("isEnableAlarmNoti = %s", isEnableAlarmNoti));
                    if(SmartViewPreference.isPushOn(context) == true && isEnableAlarmNoti){
                        Intent intent = new Intent(context, AlarmHistoryActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        String title = String.format("%s - %s", context.getString(R.string.app_name), alarmVo.getTitle());
                        notificationCenter.showNotification(context, alarmVo.getAlarmMessage(), title, alarmVo.getAlarmMessage(), intent);
                        notificationCenter.showCustomToast(title, alarmVo.getAlarmMessage(), Toast.LENGTH_LONG);
                    }
                    int unReadAlarmCount = dbHandler.getUnReadAlarmCount();
                    CommonUtil.applyUnReadAlarmCountToBadge(context, unReadAlarmCount);

                    AlarmVo recentAlarmVo = dbHandler.getRecentAlarmVo();
                    logger.i("recentAlarmVo-->" + recentAlarmVo.getIdx());
                    if(recentAlarmVo != null){
                        Intent intent = new Intent(Constants.ACTION_NEW_ALARM_ARRIVED);
                        intent.putExtra(AlarmVo.EXT_ALARMVO, recentAlarmVo);
                        context.sendBroadcast(intent);
                    }
                }else{
                    logger.w(String.format("Insert payload error[%d]", result));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private String getAlarmTitle(String alarmCode){
        String json = SmartViewPreference.getSettingText(context, null);
        if(json != null){
            Gson gson = new Gson();
            Type type = new TypeToken<List<AlarmSettingVo>>(){}.getType();
            List<AlarmSettingVo> list = gson.fromJson(json, type);
            if(list != null) {
                for (AlarmSettingVo vo : list) {
                    if(alarmCode.equals(vo.getAlarmCode())){
                        return vo.getAlarmName();
                    }
                }
            }
        }
        return null;
    }
}
