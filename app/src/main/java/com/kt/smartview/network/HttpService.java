package com.kt.smartview.network;

import com.kt.smartview.common.Constants;
import com.kt.smartview.support.log.YWMLog;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

/**
 * @author jkjeon.
 * @project SmartView.
 * @date 2016-12-16.
 */
public class HttpService {

    private static final YWMLog logger = new YWMLog(HttpService.class);

    private static ApiService apiService;
    private static ApiService enginApiService;

    public static void init(){
        apiService = createService(Constants.HOST_URL);
        enginApiService = createService(Constants.ENGIN_HOST_URL);
    }

    private static ApiService createService(String baseUrl) {
        OkHttpClient okHttpClient = getHttpClient();
        Retrofit retrofit = RetrofitHelper.getInstance().getDefaultRetrofit(okHttpClient, baseUrl);
        ApiService apiService = retrofit.create(ApiService.class);
        return apiService;
    }

    private static OkHttpClient getHttpClient(){
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.readTimeout(15, TimeUnit.SECONDS);
        httpClient.connectTimeout(15, TimeUnit.SECONDS);
        httpClient.addInterceptor(logging);
        OkHttpClient okHttpClient = httpClient.build();
        return okHttpClient;
    }

    public static ApiService getApiService() {
        if (apiService == null) {
            apiService = createService(Constants.HOST_URL);
        }
        return apiService;
    }

    public static ApiService getEnginApiService() {
        if (enginApiService == null) {
            enginApiService = createService(Constants.ENGIN_HOST_URL);
        }
        return enginApiService;
    }
}
