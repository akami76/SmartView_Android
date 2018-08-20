package com.kt.smartview.ui.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kt.smartview.R;
import com.kt.smartview.utils.CommonUtil;
import com.kt.smartview.support.log.YWMLog;
import com.kt.smartview.network.HttpCallback;
import com.kt.smartview.network.HttpEnginCallback;
import com.kt.smartview.network.HttpService;
import com.kt.smartview.support.preference.SettingPreference;
import com.kt.smartview.support.preference.SmartViewPreference;
import com.kt.smartview.ui.activity.SettingAlarmActivity;
import com.kt.smartview.vo.AlarmSettingVo;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class SettingAlarmFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, SettingAlarmActivity.OnBackkeyPressedListener {

    private static final YWMLog logger = new YWMLog(SettingAlarmFragment.class);
    private ProgressDialog progressDialog;
    private Preference alarm_mode;

    public static SettingAlarmFragment getInstance() {
        return new SettingAlarmFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        displaySettingLayout();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public void onResume() {
        super.onResume();
        ((SettingAlarmActivity) getActivity()).setOnBackkeyPressedListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        ((SettingAlarmActivity) getActivity()).setOnBackkeyPressedListener(null);
    }

    @Override
    public void onBackkeyPressed() {
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        logger.d("onPreferenceChange key : " + preference.getKey());
        if(preference.getKey().equals(getString(R.string.key_push_on))){
            if(SmartViewPreference.isPushOn(getContext())){
                CommonUtil.getGlobalApplication(getContext()).startPushManager();
            }else{
                CommonUtil.getGlobalApplication(getContext()).stopPushManager();
            }
        }
        return true;
    }

    private PreferenceScreen getPrefScreen(String json){
        /*PreferenceScreen screen = getPreferenceManager().inflateFromResource(getContext(), R.xml.preference_alarm, null);
        PreferenceCategory preferenceCategory = (PreferenceCategory)screen.findPreference(getString(R.string.key_alarm_category));*/

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getContext());
        CheckBoxPreference parentPreference = new CheckBoxPreference(getContext());
        parentPreference.setKey(getString(R.string.key_push_on));
        parentPreference.setTitle(getString(R.string.push_on));
        parentPreference.setDefaultValue(true);
        parentPreference.setSummary(getString(R.string.alarm_push_on_description));
        parentPreference.setSummaryOff(getString(R.string.alarm_push_on_description));
        parentPreference.setOnPreferenceChangeListener(this);
        screen.addPreference(parentPreference);

        PreferenceCategory preferenceCategory = new PreferenceCategory(getContext());
        preferenceCategory.setTitle(getString(R.string.general_alarm));
        preferenceCategory.setLayoutResource(R.layout.item_preference_category);
        preferenceCategory.setKey(getString(R.string.key_alarm_category));
        screen.addPreference(preferenceCategory);

        if(json != null){
            Gson gson = new Gson();
            Type type = new TypeToken<List<AlarmSettingVo>>(){}.getType();
            List<AlarmSettingVo> list = gson.fromJson(json, type);
            if(list != null){
                for(AlarmSettingVo vo : list){
                    CheckBoxPreference checkBoxPreference = new CheckBoxPreference(getContext());
                    checkBoxPreference.setKey(vo.getAlarmCode());
                    checkBoxPreference.setTitle(vo.getAlarmName());
                    checkBoxPreference.setChecked(SettingPreference.getBoolean(getContext(), vo.getAlarmCode(), true));
                    checkBoxPreference.setSummary(vo.getAlarmDesc());
                    checkBoxPreference.setSummaryOff(vo.getAlarmDesc());
                    checkBoxPreference.setDefaultValue(true);
                    preferenceCategory.addPreference(checkBoxPreference);
                }
            }
        }
        return screen;
    }

    private void applyCheckBoxDependency(){
        PreferenceCategory preferenceCategory = (PreferenceCategory)getPreferenceScreen().findPreference(getString(R.string.key_alarm_category));
        if(preferenceCategory != null){
            String json = SmartViewPreference.getSettingText(getContext(), null);
            if(json != null){
                Gson gson = new Gson();
                Type type = new TypeToken<List<AlarmSettingVo>>(){}.getType();
                List<AlarmSettingVo> list = gson.fromJson(json, type);
                if(list != null){
                    for(AlarmSettingVo vo : list){
                        CheckBoxPreference checkBoxPreference = (CheckBoxPreference)preferenceCategory.findPreference(vo.getAlarmCode());
                        if(checkBoxPreference != null){
                            checkBoxPreference.setDependency(getString(R.string.key_push_on));
                        }
                    }
                }
            }
        }
    }

    private void loadAlarmSettings(){
        progressDialog = ProgressDialog.show(getContext(), getString(R.string.app_name), getString(R.string.alarm_progress));
        Call<List<AlarmSettingVo>> req = HttpService.getEnginApiService().getAlarmSettings();
        HttpEnginCallback callback = new HttpEnginCallback(new HttpCallback.HttpCallbackListener() {
            @Override
            public void onSuccess(Object response) {
                progressDialog.dismiss();
                if(response == null){
                    errorAndFinish(getString(R.string.message_service_failed_request_otpcode));
                }else{
                    List<AlarmSettingVo> settingItems = (List<AlarmSettingVo>) response;
                    if(settingItems != null && settingItems.size() > 0){
                        Gson gson = new Gson();
                        String json = gson.toJson(settingItems);
                        SmartViewPreference.setSettingText(getContext(), json);
                        // 화면에 적용한다.
                        displaySettingLayout();
                        startPushService();
                    }else{
                        errorAndFinish(getString(R.string.message_service_failed_request_otpcode));
                    }
                }
            }

            @Override
            public void onFail(String failMessage, @Nullable Call call, @Nullable Throwable t) {
                progressDialog.dismiss();
                errorAndFinish(failMessage);
            }
        });
        req.enqueue(callback);
    }

    private void errorAndFinish(String message){
        CommonUtil.toast(getContext(), message, Toast.LENGTH_LONG);
    }
    private void startPushService(){
        if(SmartViewPreference.isPushOn(getContext())){
            CommonUtil.getGlobalApplication(getContext()).startPushManager();
        }
    }

    private void displaySettingLayout(){
        setPreferenceScreen(null);
        String json = SmartViewPreference.getSettingText(getContext(), null);
        if(json != null){
            setPreferenceScreen(getPrefScreen(json));
            applyCheckBoxDependency();
        }else{
            loadAlarmSettings();
        }
    }
}
