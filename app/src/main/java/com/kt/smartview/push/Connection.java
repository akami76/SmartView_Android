package com.kt.smartview.push;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public final class Connection {
	private Connection() {
		throw new AssertionError("This class should not be instantiated.");
	}

	public final static boolean isConnected(Context context) {
		boolean connected = false;

		final ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		final NetworkInfo[] networkInfos = connectivityManager
				.getAllNetworkInfo();

		for (int i = 0; i < networkInfos.length && !connected; i++) {
			connected = networkInfos[i].isConnectedOrConnecting();
		}

		return connected;
	}

	public final static String getHttpUserAgent() {
		return "Mozilla/5.0 (Linux; U; Android 1.6; de-ch; HTC Magic Build/DRC92) AppleWebKit/528.5+ (KHTML, like Gecko) Version/3.1.2 Mobile Safari/525.20.1";
	}
}
