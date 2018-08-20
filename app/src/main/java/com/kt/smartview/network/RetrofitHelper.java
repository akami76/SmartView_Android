package com.kt.smartview.network;

import android.util.Log;

import com.kt.smartview.GlobalApplication;
import com.kt.smartview.common.Constants;
import com.kt.smartview.utils.SSLUtil;

import java.lang.reflect.Field;

import javax.net.ssl.SSLSocketFactory;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

//import javax.net.ssl.HostnameVerifier;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class RetrofitHelper {

    private static final String TAG = "RetrofitHelper";
    private static RetrofitHelper mInstance;
    private Retrofit mRetrofit;

    private RetrofitHelper() {
    }

    public static RetrofitHelper getInstance() {
        if (mInstance == null) {
            synchronized (RetrofitHelper.class) {
                mInstance = new RetrofitHelper();
            }
        }
        return mInstance;
    }

    public Retrofit getDefaultRetrofit(OkHttpClient httpClient, final String baseUrl) {
        if (mRetrofit != null) {
            Log.d(TAG, "Retrofit is already initialized");
            return mRetrofit;
        }else{
            try {
                SSLSocketFactory socketFactory = SSLUtil.getSocketFactory(GlobalApplication.getContext(), Constants.SSL_CLIENT_CA_PATH, Constants.SSL_CLIENT_CRT_PATH, Constants.SSL_SERVER_KEY_PATH, Constants.SSL_CLIENT_CERT_PWD);
                if(socketFactory != null){
                    String workerClassName = "okhttp3.OkHttpClient";
                    Class workerClass = Class.forName(workerClassName);
                    Field sslSocketFactory = workerClass.getDeclaredField("sslSocketFactory");
                    sslSocketFactory.setAccessible(true);
                    sslSocketFactory.set(httpClient, socketFactory);
                }
                return new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(httpClient)
                        .build();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /*private SSLContext getSSLContext(String crtFilePath) {
        try{
            byte[] derBytes = CommonUtil.getBytesFromFile(GlobalApplication.getContext(), crtFilePath);

            ByteArrayInputStream derInputStream = new ByteArrayInputStream(derBytes);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(derInputStream);
            String alias = cert.getSubjectX500Principal().getName();

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry(alias, cert);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca-certificate", cert);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
            kmf.init(keyStore, Constants.SSL_CLIENT_CERT_PWD.toCharArray());
            KeyManager[] keyManagers = kmf.getKeyManagers();

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(trustStore);
            TrustManager[] trustManager = tmf.getTrustManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManager, null);
            return sslContext;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }*/
}