package com.kt.smartview.network;

import com.kt.smartview.vo.AlarmSettingVo;
import com.kt.smartview.vo.ResultVo;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * @author jkjeon.
 * @project SmartView.
 * @date 2016-12-16.
 */
public interface ApiService {

    // 로그인
    @FormUrlEncoded
    @POST("app/mobileLogin")
    Call<ResultVo> requestLogin(
            @Field("loginUserId") String user_id,
            @Field("loginPassword") String password,
            @Field("otpCode") String otpCode,
            @Field("deviceKey") String deviceKey
    );

    // OTP코드 요청
    @POST("apms-web-client/app/requestAuthCode")
    Call<ResultVo> requestAuthCode(
            @Header("Content-Type") String content_type,
            @Body Map<String, Object> param
    );

    // 메뉴 요청
    @FormUrlEncoded
    @POST("apms-web-client/app/menuList")
    Call<ResultVo> getMenuList(
            @Field("deviceKey") String deviceKey
    );

    // 알람설정 요청
    @GET("apms-engine/rest/mqtt/noti/alarm-code")
    Call<List<AlarmSettingVo>> getAlarmSettings(
    );


}
