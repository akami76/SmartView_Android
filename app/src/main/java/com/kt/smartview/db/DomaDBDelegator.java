package com.kt.smartview.db;

import android.content.Context;
import android.database.Cursor;

import com.kt.smartview.GlobalApplication;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public abstract class DomaDBDelegator {
	DomaDBAdapter readableDBAdapter;
	DomaDBAdapter writableDBAdapter;

	protected DomaDBDelegator(Context context) {
		readableDBAdapter = ((GlobalApplication) context.getApplicationContext()).getReadableDBAdapter();
		writableDBAdapter = ((GlobalApplication) context.getApplicationContext()).getWritableDBAdapter();
	}

	public boolean open() {
		if(readableDBAdapter.isOpen() == false){
			readableDBAdapter.open(true);
		}
		return readableDBAdapter.isOpen();
	}

	/*public void close() {
		if(readableDBAdapter != null && readableDBAdapter.isOpen()){
			readableDBAdapter.close();
		}
	}*/

	public void closeCursorOnly(Cursor cursor) {
		if (cursor != null) {
			cursor.close();
		}
	}

	public void close(Cursor cursor) {
		closeCursorOnly(cursor);
		//close();
	}

	public DomaDBAdapter getWritableDBAdapter(){
		return writableDBAdapter;
	}

	public DomaDBAdapter getReadableDBAdapter(){
		return readableDBAdapter;
	}
}
