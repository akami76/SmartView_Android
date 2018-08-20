package com.kt.smartview.network;

import android.content.Context;

import com.kt.smartview.GlobalApplication;
import com.kt.smartview.R;
import com.kt.smartview.support.log.YWMLog;
import com.kt.smartview.vo.AlarmSettingVo;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author jkjeon.
 * @project SmartView.
 * @date 2016-12-16.
 */
public class HttpEnginCallback implements Callback {

    private static final YWMLog logger = new YWMLog(HttpEnginCallback.class);

    private HttpCallback.HttpCallbackListener httpCallbackListener;

    public HttpEnginCallback(HttpCallback.HttpCallbackListener httpCallbackListener) {
        this.httpCallbackListener = httpCallbackListener;
    }

    public HttpCallback.HttpCallbackListener getHttpCallbackListener() {
        return httpCallbackListener;
    }

    public void setHttpCallbackListener(HttpCallback.HttpCallbackListener httpCallbackListener) {
        this.httpCallbackListener = httpCallbackListener;
    }

    @Override
    public void onResponse(Call call, Response response) {
        try{
            logger.d("retrofit callback onRespose response code : " + response.code() + "  // raw.code : " + response.raw().code());
            logger.d("retrofit callback onRespose message : " + response.message());
            Context context = GlobalApplication.getContext();

            if (response.code() == 404) {
                httpCallbackListener.onFail(context.getString(R.string.network_error_404), call, null);
                return ;
            }
            List<AlarmSettingVo> res = (List<AlarmSettingVo>)response.body();
            if (res == null) {
                httpCallbackListener.onFail(context.getString(R.string.network_error_204), call, null);
                return ;
            }
            httpCallbackListener.onSuccess(res);
        } catch (Exception e) {
            e.printStackTrace();
            httpCallbackListener.onFail(e.getMessage(), call, null);
        }
    }

    @Override
    public void onFailure(Call call, Throwable t) {
        if (t != null) {
            t.printStackTrace();
        }
        Context context = GlobalApplication.getContext();
        httpCallbackListener.onFail(GlobalApplication.getContext().getString(R.string.network_error_000), call, t);
    }
}
