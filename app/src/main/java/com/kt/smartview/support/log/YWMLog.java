package com.kt.smartview.support.log;

import android.util.Log;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class YWMLog {
	public static int logLevel = Log.DEBUG;
//	public static int logLevel = 15;		// Production;
	private String tag;
	private final String EMPTY = "";
	
	public YWMLog() {
		// TODO Auto-generated constructor stub
	}
	public YWMLog(Class<?> object) {
		// TODO Auto-generated constructor stub
		this.tag = object.getSimpleName();
	}
	
	public int v(String format, Object... args) {
		if(logLevel > Log.VERBOSE){
			return -1;
		}
		return Log.v(tag, format(format, args));
	}

	public int v(String msg, Throwable e) {
		if(logLevel > Log.VERBOSE){
			return -1;
		}
		return Log.v(tag, msg, e);
	}

	public int v(String format, Throwable e, Object... args) {
		if(logLevel > Log.VERBOSE){
			return -1;
		}
		return Log.v(tag, format(format, args), e);
	}

	public int d(String format, Object... args) {
		if(logLevel > Log.DEBUG){
			return -1;
		}
		return Log.d(tag, format(format, args));
	}

	public int d(String msg, Throwable e) {
		if(logLevel > Log.DEBUG){
			return -1;
		}
		return Log.d(tag, msg, e);
	}

	public int d(String format, Throwable e, Object... args) {
		if(logLevel > Log.DEBUG){
			return -1;
		}
		return Log.d(tag, format(format, args), e);
	}

	public int w(String format, Object... args) {
		if(logLevel > Log.WARN){
			return -1;
		}
		return Log.w(tag, format(format, args));
	}

	public int w(String msg, Throwable e) {
		if(logLevel > Log.WARN){
			return -1;
		}
		return Log.w(tag, msg, e);
	}

	public int w(String format, Throwable e, Object... args) {
		if(logLevel > Log.WARN){
			return -1;
		}
		return Log.w(tag, format(format, args), e);
	}

	public int i(String format, Object... args) {
		if(logLevel > Log.INFO){
			return -1;
		}
		return Log.i(tag, format(format, args));
	}

	public int i(String msg, Throwable e) {
		if(logLevel > Log.INFO){
			return -1;
		}
		return Log.i(tag, msg, e);
	}

	public int i(String format, Throwable e, Object... args) {
		if(logLevel > Log.INFO){
			return -1;
		}
		return Log.i(tag, format(format, args), e);
	}

	public int e(String format, Object... args) {
		if(logLevel > Log.ERROR){
			return -1;
		}
		return Log.e(tag, format(format, args));
	}

	public int e(String msg, Throwable e) {
		if(logLevel > Log.ERROR){
			return -1;
		}
		return Log.e(tag, msg, e);
	}

	public int e(String format, Throwable e, Object... args) {
		if(logLevel > Log.ERROR){
			return -1;
		}
		return Log.e(tag, format(format, args), e);
	}

	private String format(String format, Object... args) {
		try {
			return String.format(format == null ? EMPTY : format, args);
		} catch (RuntimeException e) {
			w(tag, "format error. reason=%s, format=%s", e.getMessage(), format);
			return String.format(EMPTY, format);
		}
	}
}
