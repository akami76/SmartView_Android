package com.kt.smartview.ui.fragment;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.gson.internal.LinkedTreeMap;
import com.kt.smartview.R;
import com.kt.smartview.network.HttpCallback;
import com.kt.smartview.network.HttpService;
import com.kt.smartview.support.permission.PermissionManager;
import com.kt.smartview.support.preference.SmartViewPreference;
import com.kt.smartview.ui.activity.MainActivity;
import com.kt.smartview.utils.CommonUtil;
import com.kt.smartview.utils.DialogUtils;
import com.kt.smartview.utils.SnackbarUtils;
import com.kt.smartview.vo.LoginVo;
import com.kt.smartview.vo.ResultVo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class LoginFragment extends BaseFragment implements View.OnClickListener, TextView.OnEditorActionListener {

    private Button btn_login, btn_otp_code;
    private TextView text_id, text_pw, text_otp_code;
    private int currentExpireTime = 0;
    private ScheduledFuture<?> timeScheduleHandler;
    private ScheduledExecutorService timeScheduler = Executors
            .newScheduledThreadPool(1);
    private boolean isRunningOtpCode = false;
    private ProgressDialog progressDialog;
    private SmsReceiver smsReceiver;
    public static final int REQUEST_CODE_FOR_SMS = 1;
    private LoginActionListener loginActionListener;
    private CheckBox chk_save_id;

    public interface LoginActionListener{
        public void login(LoginVo loginVo);
    }

    public LoginFragment(){
    }

    public static LoginFragment getInstance(){
        LoginFragment fragment = new LoginFragment();
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View contentsView = inflater.inflate(R.layout.content_login, null);

        text_id = (TextView)contentsView.findViewById(R.id.text_id);
        text_pw = (TextView)contentsView.findViewById(R.id.text_pw);
        text_otp_code = (TextView)contentsView.findViewById(R.id.text_otp_code);
        chk_save_id = (CheckBox)contentsView.findViewById(R.id.chk_save_id);

        btn_login = (Button) contentsView.findViewById(R.id.btn_login);
        btn_otp_code = (Button)contentsView.findViewById(R.id.btn_otp_code);

        btn_login.setOnClickListener(this);
        btn_otp_code.setOnClickListener(this);
        text_otp_code.setOnEditorActionListener(this);
        text_pw.setOnEditorActionListener(this);
        chk_save_id.setChecked(SmartViewPreference.isSaveId(getContext()));

        return contentsView;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(hidden == false){
//            resetLayout();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        regReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimer();
        if(timeScheduler != null){
            timeScheduler.shutdown();
        }
        if(smsReceiver != null){
            getActivity().unregisterReceiver(smsReceiver);
            smsReceiver = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode==REQUEST_CODE_FOR_SMS){
            if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                SnackbarUtils.show(getActivity(), R.string.message_granted_sms_received);
            }else{
                SnackbarUtils.show(getActivity(), R.string.message_fail_granted_sms_received);
            }
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_login:
                CommonUtil.hideKeyboard(getContext(), text_id);
                if(isNetworkAvailableAndAlert(getString(R.string.message_cannot_connect_network_error)) == false){
                    return;
                }
                login();
                break;
            case R.id.btn_otp_code:
                if(ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED){
                    if(isRunningOtpCode){
                        DialogUtils.showDialog(getContext(), getChildFragmentManager(), R.string.prompt_get_code, R.string.msg_stop_otp_process, R.string.confirm, true,
                                new AlertFragment.DialogListener() {
                                    @Override
                                    public void onDialogPositiveClick() {
                                        stopTimer();
                                    }

                                    @Override
                                    public void onDialogNegativeClick() {
                                    }
                                });
                    }else{
                        if(isNetworkAvailableAndAlert(getString(R.string.message_cannot_connect_network_error)) == false){
                            return;
                        }
                        requestOTPCode();
                    }
                }else{
                    PermissionManager.check(getActivity(), Manifest.permission.RECEIVE_SMS, REQUEST_CODE_FOR_SMS);
                }
                break;
        }
    }

    private boolean isNetworkAvailableAndAlert(String message){
        if(CommonUtil.isNetworkAvailable(getContext()) == false){
            DialogUtils.showDialog(getContext(), getChildFragmentManager(), getString(R.string.message_cannot_connect_network_error), null);
            return false;
        }else{
            return true;
        }

    }
    private void startAuthrizingTimer(int currentExpireTime) {
        if(isRunningOtpCode) return;
        this.currentExpireTime = currentExpireTime;
        timeScheduleHandler = timeScheduler
                .scheduleWithFixedDelay(timeRunner, 0, 1,
                        TimeUnit.SECONDS);
        isRunningOtpCode = true;
    }
    private Runnable timeRunner = new Runnable() {
        public void run() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(isRunningOtpCode == false) return;

                currentExpireTime = currentExpireTime - 1;
                if(currentExpireTime == 0){
                    stopTimer();
                }else{
                    int minute = currentExpireTime / 60;
                    int seconds = currentExpireTime % 60;
                    btn_otp_code.setText(String.format("%d%s %02d%s", minute, getString(R.string.minute), seconds, getString(R.string.second)));
                }
            }
        });
        }
    };

    private void stopTimer(){
        isRunningOtpCode = false;
        btn_otp_code.setText(getString(R.string.prompt_get_code));
        if(timeScheduleHandler != null){
            timeScheduleHandler.cancel(true);
        }
    }

    private void login(){
        String userId = text_id.getText().toString();
        String passwd = text_pw.getText().toString();
        String otpCode = text_otp_code.getText().toString();
        String deviceKey = CommonUtil.getMacAddress();
        if(TextUtils.isEmpty(userId)){
            DialogUtils.showDialog(getContext(), getChildFragmentManager(), getString(R.string.message_no_input_id), null);
            text_id.requestFocus();
            return;
        }
        if(TextUtils.isEmpty(passwd)){
            DialogUtils.showDialog(getContext(), getChildFragmentManager(), getString(R.string.message_no_input_passwd), null);
            text_pw.requestFocus();
            return;
        }
        if(TextUtils.isEmpty(otpCode)){
            DialogUtils.showDialog(getContext(), getChildFragmentManager(), getString(R.string.message_no_input_otp_code), null);
            text_otp_code.requestFocus();
            return;
        }
        if(otpCode.length() != 6 || CommonUtil.isNumber(otpCode) == false){
            DialogUtils.showDialog(getContext(), getChildFragmentManager(), getString(R.string.message_otp_code_length_error), null);
            text_otp_code.requestFocus();
            return;
        }
        CommonUtil.hideKeyboard(getContext(), text_id);
        progressDialog = ProgressDialog.show(getContext(), getString(R.string.app_name), getString(R.string.login_progress));
        LoginVo loginVo = new LoginVo(System.currentTimeMillis(), userId, passwd, otpCode, deviceKey, chk_save_id.isChecked());
        loginActionListener.login(loginVo);
    }

    public void cancelLogin(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }
    private String getRSAEncriptedDeviceKey(){
        try{
//            String androidId = CommonUtil.getAndroidId(getContext());
            String macAddress = CommonUtil.getMacAddress();
            Log.i("TEST", "macAddress-->" + macAddress);
            String encriptAndroidKey = getParentActivity().getRsaCipher().encrypt(macAddress);
            return encriptAndroidKey;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /*private String decryptText(String rawText){
        try{
            String descriptAndroidKey = getParentActivity().getRsaCipher().decrypt(rawText);
            return descriptAndroidKey;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }*/

    private void requestOTPCode(){
        String encriptAndroidKey = getRSAEncriptedDeviceKey();
        if(encriptAndroidKey == null){
            DialogUtils.showDialog(getContext(), getChildFragmentManager(), R.string.message_service_failed_request_otpcode, null);
        }else{
            progressDialog = ProgressDialog.show(getContext(), getString(R.string.app_name), getString(R.string.otp_progress));
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("deviceKey", encriptAndroidKey);
            Log.i("TEST", encriptAndroidKey);
            Call<ResultVo> req = HttpService.getApiService().requestAuthCode("application/json", params);
            HttpCallback callback = new HttpCallback(new HttpCallback.HttpCallbackListener() {
                @Override
                public void onSuccess(Object response) {
                    progressDialog.dismiss();
//                    stopTimer();
                    if(response == null){
                        DialogUtils.showDialog(getContext(), getChildFragmentManager(), getString(R.string.message_service_failed_request_otpcode), null);
                    }else{
                        ResultVo res = (ResultVo) response;
                        LinkedTreeMap<String, Object> map = (LinkedTreeMap<String, Object>)res.getData();
                        /*String otpCode = String.valueOf(map.get("encodedOtpCode"));
                        otpCode = decryptText(otpCode);
                        Log.i("TEST", "otpCode-->" + otpCode);
                        text_otp_code.setText(otpCode);*/

                        int otpStandBySec = (int)Double.parseDouble(String.valueOf(map.get("otpStandBySec")));
                        Log.i("TEST", "otpStandBySec-->" + otpStandBySec);
                        startAuthrizingTimer(otpStandBySec);
                    }
                }

                @Override
                public void onFail(String failMessage, @Nullable Call call, @Nullable Throwable t) {
                    DialogUtils.showDialog(getContext(), getChildFragmentManager(), failMessage, null);
                    progressDialog.dismiss();
                    stopTimer();
                }
            });
            req.enqueue(callback);
        }
    }


    private void regReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction ("android.provider.Telephony.SMS_RECEIVED");
        intentFilter.setPriority (99999999);
        smsReceiver = new SmsReceiver ();
        getActivity().registerReceiver(smsReceiver, intentFilter);
    }

    private class SmsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Object messages[] = (Object[]) bundle.get("pdus");
            SmsMessage smsMessage[] = new SmsMessage[messages.length];

            int smsPieces = messages.length;
            for (int n = 0; n < smsPieces; n++) {
                smsMessage[n] = SmsMessage.createFromPdu((byte[]) messages[n]);
            }
            String message = smsMessage[0].getMessageBody().toString();
            String smsTitle = String.format("%s", getString(R.string.sms_title_name));
//            Log.e("TEST", "SmsReceiver onReceive--> " + message);
//            SnackbarUtils.show(getContext(), message+"");
            if (message != null && message.indexOf(smsTitle) > -1) {
                Pattern pattern = Pattern.compile("\\d{6}");
                Matcher matcher = pattern.matcher(message);
                String authNumber = null;
                if (matcher.find()) {
                    authNumber = matcher.group(0);
                }
                if(authNumber != null){
                    text_otp_code.setText(authNumber);
                    stopTimer();
                }
            }
        }
    }

    public void setLoginActionListener(LoginActionListener loginActionListener) {
        this.loginActionListener = loginActionListener;
    }

    public void resetLayout(){
        if(SmartViewPreference.isSaveId(getContext()) && SmartViewPreference.getUserId(getContext()) != null){
            text_id.setText(SmartViewPreference.getUserId(getContext()));
        }else{
            text_id.setText("");
        }
        text_pw.setText("");
        text_otp_code.setText("");
        btn_login.setEnabled(true);
        btn_otp_code.setEnabled(true);
        stopTimer();
    }

    public void loginSuccess(LoginVo loginVo){
        if(loginVo != null){
            SmartViewPreference.setSaveId(getContext(), loginVo.isSaveId());
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if((v.getId() == R.id.text_pw || v.getId() == R.id.text_otp_code) && actionId == EditorInfo.IME_ACTION_DONE){
            btn_login.performClick();
        }
        return false;
    }

    private MainActivity getParentActivity(){
        return (MainActivity)getActivity();
    }
}
