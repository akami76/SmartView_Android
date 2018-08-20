package com.kt.smartview.common;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class Constants {
	public static final String CONFIG_TAG = "SMARTVIEW_CONFIG";

	/*
	Dev Server
	 */
//	public static final String PUSH_DOMAIN_URL = "loyaltydev.kt.com";
//	public static final String SSL_CLIENT_CRT_PATH = "certificate/loyaltydev_kt_com_cert.pem";
//	public static final String SERVER_HOST_URL = "https://loyaltydev.kt.com";

	/*
	Real Server
	 */

	// 도메인 이름
	public static final String PUSH_DOMAIN_URL = "loyalty.kt.com";
	public static final String SERVER_HOST_URL = "https://loyalty.kt.com";
	// SSL Ca 파일경로
	public static final String SSL_CLIENT_CA_PATH = "certificate/Chain_RootCA_Bundle.crt";
	// SSL cert 파일경로
	public static final String SSL_CLIENT_CRT_PATH = "certificate/loyalty_kt_com_00001_cert.pem";
	// SSL key 파일경로
	public static final String SSL_SERVER_KEY_PATH = "certificate/server.key";
	// SSL 인증서 암호
	public static final String SSL_CLIENT_CERT_PWD = "45hjs341";
	// RSA 암호화 public key 파일경로
	public static final String RSA_PUBLIC_KEY_PATH = "rsa/public.key";

	public static final int LIST_REFRESH = 0;
	public static final int LIST_LOADMORE = 1;

	public static final String ENGIN_HOST_URL = SERVER_HOST_URL + ":61145/";
	public static final String ENGIN_ROOT_URL = "apms-engine/";

	public static final String HOST_URL = SERVER_HOST_URL + ":61146/";
	public static final String ROOT_URL = HOST_URL + "apms-web-client/";

	public static final String SITE_MAIN_ACTION_URL = ROOT_URL + "dashboard";
	public static final String SITE_LOGOUT_ACTION_URL = ROOT_URL + "main/login/logoutAction";
	public static final String SITE_LOGIN_ACTION_URL = ROOT_URL + "main/login/loginProcessAction";
	public static final String SITE_LOGIN_PAGE_URL = ROOT_URL + "main/login";

	public static final boolean isEnableSwipeRefresh = false;

	public static final String ACTION_NEW_ALARM_ARRIVED = "ACTION_NEW_ALARM_ARRIVED";
}
