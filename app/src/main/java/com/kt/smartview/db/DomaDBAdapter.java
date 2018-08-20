package com.kt.smartview.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class DomaDBAdapter {
	private final Context mContext;
	private SQLiteDatabase mDB;
	private DomaDBHelper dbHelper;
	public DomaDBAdapter(Context context, boolean isReadable) {
		mContext = context;
		dbHelper = DomaDBHelper.getInstance(context);
		open(isReadable);
	}
	public synchronized void closeDatabase() {
		if (mDB != null) {
			mDB.close();
		}
		dbHelper.close();
	}

	public boolean open(boolean isReadable) throws SQLException {
		if(isReadable){
			mDB = dbHelper.getReadableDatabase();
		}else{
			mDB = dbHelper.getWritableDatabase();
		}
		return mDB != null ? true : false;
	}

	public List cursor2List(Cursor c) {
		List list = new ArrayList();
		if (c != null && c.getCount() > 0) {
			c.moveToFirst();
			do {
				Map map = new HashMap();
				for (int j = 0; j < c.getColumnCount(); j++) {
					map.put(c.getColumnName(j), c.getString(j));
				}
				list.add(map);
			} while (c.moveToNext());
		}
		return list;
	}

	public Map cursor2Map(Cursor c) {
		Map map = null;
		c.moveToFirst();
		if (c != null && c.getCount() > 0) {
			map = new HashMap();
			for (int i = 0; i < c.getColumnCount(); i++) {
				map.put(c.getColumnName(i), c.getString(i));
			}
		}
		return map;
	}

	public List select(String query) throws SQLException {
		Cursor c = mDB.rawQuery(query, null);
		List list = null;
		c.moveToFirst();

		if (c != null && c.getCount() > 0) {
			list = new ArrayList();
			do {
				Map map = new HashMap();
				for (int i = 0; i < c.getColumnCount(); i++) {
					map.put(c.getColumnName(i), c.getString(i));
				}
				list.add(map);
			} while (c.moveToNext());
		}
		c.close();
		return list;
	}

	public Map<String, String> selectOne(String query) throws SQLException {
		Cursor c = mDB.rawQuery(query, null);
		Map map = null;
		c.moveToFirst();
		if (c != null && c.getCount() > 0) {
			map = new HashMap();
			for (int i = 0; i < c.getColumnCount(); i++) {
				map.put(c.getColumnName(i), c.getString(i));
			}
		}
		c.close();
		return map;
	}

	public void executeNoneQuery(String query) throws SQLException {
		mDB.execSQL(query);
	}

	public void executeNoneQuery(String query, String[] params) throws SQLException {
		mDB.execSQL(query, params);
	}

	public Cursor executeQuery(String query) throws SQLException {
		return executeQuery(query, null);
	}

	public Cursor executeQuery(String query, String[] params) throws SQLException {
		return mDB.rawQuery(query, params);
	}

	public boolean isOpen() {
		if (mDB == null)
			return false;
		return mDB.isOpen();
	}

	public boolean isReadOnly() {
		if (mDB == null)
			return false;
		return mDB.isReadOnly();
	}

	public void startTransaction() {
		if (mDB != null) {
			mDB.beginTransaction();
		}
	}

	public void endTransaction() {
		if (mDB != null && mDB.inTransaction()) {
			mDB.endTransaction();
		}
	}

	public void commitTransaction() {
		if (mDB != null && mDB.inTransaction()) {
			mDB.setTransactionSuccessful();
		}
	}

	public long insert(String table, String nullColumnHack, ContentValues values) throws SQLException{
		long re_val = 0;
		re_val = mDB.insert(table, nullColumnHack, values);
		return re_val;
	}

	public int update(String tableName, ContentValues updateValues, String where, String[] whereArgs) throws SQLException{
		int re_val = 0;
		re_val = mDB.update(tableName, updateValues, where, whereArgs);
		return re_val;
	}
	public int delete(String tableName, String whereClause, String[] whereArgs) throws SQLException{
		int re_val = 0;
		re_val = mDB.delete(tableName, whereClause, whereArgs);
		return re_val;
	}
	public Cursor executeQuery(String tableName, String[] columns,
			String selection, String[] selectionArgs, String groupBy,
			String having, String orderBy) throws SQLException{
		return mDB.query(tableName, columns, selection, selectionArgs, groupBy,
				having, orderBy);
	}

	public Cursor executeQuery(String tableName, String[] columns,
			String selection, String[] selectionArgs) throws SQLException {
		return mDB.query(tableName, columns, selection, selectionArgs, null,
				null, null);
	}

	public Cursor executeQuery(String tableName, String[] columns,
			String selection, String[] selectionArgs, String groupBy,
			String orderBy) throws SQLException{
		return mDB.query(tableName, columns, selection, selectionArgs, groupBy,
				null, orderBy);
	}
	public Cursor executeQuery(String tableName, String[] columns,
							   String selection, String[] selectionArgs, String groupBy, String having,
							   String orderBy, String limit) throws SQLException{
		return mDB.query(tableName, columns, selection, selectionArgs, groupBy,
				having, orderBy, limit);
	}
	/*public void initialize(){
		String qry = "Create Table If Not EXISTS " + DotocDBHandler.PUSH_TOPIC_LIST + " (dotoc_dialog_room_id varchar(128) primary key,"
				+ "topic_prefix varchar (64),"
				+ "topic_suffix varchar (64),"
				+ "topic varchar(256))";
		mDB.execSQL(qry);
	}*/
}
