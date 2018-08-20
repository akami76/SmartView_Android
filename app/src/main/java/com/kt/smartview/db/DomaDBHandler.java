package com.kt.smartview.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.text.TextUtils;
import android.util.Log;

import com.kt.smartview.support.log.YWMLog;
import com.kt.smartview.vo.AlarmVo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class DomaDBHandler extends DomaDBDelegator {
    private final YWMLog logger = new YWMLog(DomaDBHandler.class);
    public static final String TABLE_ALARM_HISTORY = "alarm_history";
    public static final String TABLE_SCHEMA_MIGRATIONS = "schema_migrations";

    /*
        alarm_history 테이블에 대한 Column
     */
    public static final String COLUMN_PH_IDX = "idx";
    public static final String COLUMN_PH_PUSH_KEY = "push_key";
    public static final String COLUMN_PH_ALARM_CODE = "alarm_code";
    public static final String COLUMN_PH_TITLE = "title";
    public static final String COLUMN_PH_ALARM_MESSAGE = "alarm_message";
    public static final String COLUMN_PH_OCCUR_TIME = "occur_time";
    public static final String COLUMN_PH_RECEIVE_DATE = "receive_date";
    public static final String COLUMN_PH_IS_READ_OK = "is_read_ok";
    public static final String COLUMN_PH_IS_CLICK_OK = "is_click_ok";



    private final String[] alarm_history_column = new String[]{
            COLUMN_PH_IDX,
            COLUMN_PH_PUSH_KEY,
            COLUMN_PH_ALARM_CODE,
            COLUMN_PH_TITLE,
            COLUMN_PH_ALARM_MESSAGE,
            COLUMN_PH_OCCUR_TIME,
            COLUMN_PH_RECEIVE_DATE,
            COLUMN_PH_IS_READ_OK,
            COLUMN_PH_IS_CLICK_OK
    };
    private Context context;
    public DomaDBHandler(Context context) {
        // TODO Auto-generated constructor stub
        super(context);
        this.context = context;
    }

    public int getCurrentDBVersion() {
        int result = 0;
        Cursor cursor = null;
        try {
            if (open()) {
                String query = "SELECT VERSION  FROM SCHEMA_MIGRATIONS";
                cursor = readableDBAdapter.executeQuery(query);
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getInt(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            close(cursor);
        }
        return result;
    }

    private String getPagingLimit(int pageNumber, int pageSize){
        return pageSize + " OFFSET " + (pageNumber - 1) * pageSize;
    }

    public void clearTable(String tableName){
        try {
            writableDBAdapter.startTransaction();
            writableDBAdapter.delete(tableName, null, null);
            writableDBAdapter.commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writableDBAdapter.endTransaction();
        }
    }
    public void updateDBVersion(int version) {
        String query;
        try {
            writableDBAdapter.startTransaction();
            query = "UPDATE SCHEMA_MIGRATIONS SET VERSION = ?";
            writableDBAdapter.executeNoneQuery(query, new String[]{String.valueOf(version)});
            writableDBAdapter.commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writableDBAdapter.endTransaction();
        }
    }

    public void createMigrationTable(){
        try{
            writableDBAdapter.startTransaction();
            String qry = "Create Table If Not EXISTS schema_migrations (version INTEGER NOT NULL)";
            writableDBAdapter.executeNoneQuery(qry);
            writableDBAdapter.commitTransaction();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            writableDBAdapter.endTransaction();
        }
    }

    public void insertVersion(int version){
        try{
            writableDBAdapter.startTransaction();
            ContentValues insertVlueset = new ContentValues();
            insertVlueset.put("version ", version );
            writableDBAdapter.insert(TABLE_SCHEMA_MIGRATIONS, null, insertVlueset);
            writableDBAdapter.commitTransaction();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            writableDBAdapter.endTransaction();
        }
    }


    public List<AlarmVo> getAlarmHistoryListAll(int pageNumber, int pageSize){
        List<AlarmVo> list = new ArrayList<AlarmVo>();
        Cursor cursor = null;
        try {
            if (open()) {

                cursor = readableDBAdapter.executeQuery(
                        TABLE_ALARM_HISTORY,
                        alarm_history_column,
                        null,
                        null,
                        null,
                        null,
                        COLUMN_PH_OCCUR_TIME + " desc" + " LIMIT " + getPagingLimit(pageNumber, pageSize)
                );
                if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
                    do {
                        AlarmVo vo = getPushMessageFromCursor(cursor);
                        list.add(vo);
                    } while (cursor.moveToNext());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return list;
        } finally {
            close(cursor);
        }
        return list;
    }
    public List<AlarmVo> getRecentAlarmHistoryList(long idx){
        List<AlarmVo> list = new ArrayList<AlarmVo>();
        Cursor cursor = null;
        try {
            if (open()) {
                cursor = readableDBAdapter.executeQuery(
                        TABLE_ALARM_HISTORY,
                        alarm_history_column,
                        COLUMN_PH_IDX + " > ?",
                        new String[]{String.valueOf(idx)},
                        null,
                        null,
                        null
                );
                if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
                    do {
                        AlarmVo vo = getPushMessageFromCursor(cursor);
                        list.add(vo);
                    } while (cursor.moveToNext());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return list;
        } finally {
            close(cursor);
        }
        return list;
    }
    public AlarmVo getAlarmHistory(long idx){
        AlarmVo vo = null;
        Cursor cursor = null;
        try {
            if (open()) {
                cursor = readableDBAdapter.executeQuery(
                        TABLE_ALARM_HISTORY,
                        alarm_history_column,
                        COLUMN_PH_IDX + "=?",
                        new String[]{String.valueOf(idx)},
                        null,
                        null,
                        null
                );
                if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
                    vo = getPushMessageFromCursor(cursor);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return vo;
        } finally {
            close(cursor);
        }
        return vo;
    }
    public List<AlarmVo> getAlarmHistory(String[] strInIdx){
        List<AlarmVo> list = new ArrayList<AlarmVo>();
        Cursor cursor = null;
        try {
            if (open()) {
                cursor = readableDBAdapter.executeQuery(
                        TABLE_ALARM_HISTORY,
                        alarm_history_column,
                        COLUMN_PH_IDX + " IN (" +
                                TextUtils.join(",", Collections.nCopies(strInIdx.length, "?")) +
                                ")", strInIdx,
                        null,
                        null,
                        null
                );
                if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
                    do {
                        AlarmVo vo = getPushMessageFromCursor(cursor);
                        list.add(vo);
                    } while (cursor.moveToNext());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return list;
        } finally {
            close(cursor);
        }
        return list;
    }

    public int setUnClickAlarm(long idx) {
        int result = -1;
        try {
            writableDBAdapter.startTransaction();
            ContentValues insertVlueset = new ContentValues();
            insertVlueset.put(COLUMN_PH_IS_CLICK_OK, "Y");
            insertVlueset.put(COLUMN_PH_IS_READ_OK, "Y");
            result = writableDBAdapter.update(TABLE_ALARM_HISTORY, insertVlueset,
                    COLUMN_PH_IDX + "=?", new String[]{String.valueOf(idx)});
            writableDBAdapter.commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        } finally {
            writableDBAdapter.endTransaction();
        }
        return result;
    }

    public int setReadAlarmOK(String[] strInIdx) {
        int result = 0;
        try {
            writableDBAdapter.startTransaction();
            ContentValues insertVlueset = new ContentValues();
            insertVlueset.put(COLUMN_PH_IS_READ_OK, "Y");
            result = writableDBAdapter.update(TABLE_ALARM_HISTORY, insertVlueset, COLUMN_PH_IDX + " IN (" +
                    TextUtils.join(",", Collections.nCopies(strInIdx.length, "?")) +
                    ")", strInIdx);
            Log.d("TEST", "setReadAlarmOK result-->" + result);
            writableDBAdapter.commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        } finally {
            writableDBAdapter.endTransaction();
        }
        return result;
    }

    public int setUnReadAlarm(AlarmVo vo) {
        int result = -1;
        try {
            writableDBAdapter.startTransaction();
            ContentValues insertVlueset = new ContentValues();
            insertVlueset.put(COLUMN_PH_IS_READ_OK, vo.getIsReadOk());
            result = writableDBAdapter.update(TABLE_ALARM_HISTORY, insertVlueset, COLUMN_PH_IDX + "=?", new String[]{String.valueOf(vo.getIdx())});
            writableDBAdapter.commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        } finally {
            writableDBAdapter.endTransaction();
        }
        return result;
    }

    public int getUnClickAlarmCount(String pushTypeArgs){
        Cursor cursor = null;
        int unClickCount = 0;
        try {
            if (open()) {
                String query = "select count(" + COLUMN_PH_PUSH_KEY + ") as unclick_count from " + TABLE_ALARM_HISTORY
                        + " where "
                        + COLUMN_PH_IS_CLICK_OK + " <> 'Y'";
                if(TextUtils.isEmpty(pushTypeArgs) == false){
                    query = query + " AND " + COLUMN_PH_ALARM_CODE + " IN (" + pushTypeArgs + ")";
                }
                cursor = readableDBAdapter.executeQuery(query);
                if (cursor != null && cursor.moveToFirst()) {
                    unClickCount = cursor.getInt(0);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return unClickCount;
        } finally {
            close(cursor);
        }
        return unClickCount;
    }

    public String getLastAlarmDate(){
        Cursor cursor = null;
        String maxCreateDate = null;
        try {
            if (open()) {
                String query = "select max(" + COLUMN_PH_OCCUR_TIME + ") from " + TABLE_ALARM_HISTORY;
                cursor = readableDBAdapter.executeQuery(query);
                if (cursor != null && cursor.moveToFirst()) {
                    maxCreateDate = cursor.getString(0);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return maxCreateDate;
        } finally {
            close(cursor);
        }
        return maxCreateDate;
    }

    public int getUnReadAlarmCount(){
        Cursor cursor = null;
        int unReadCount = 0;
        try {
            if (open()) {
                String query = "select count(" + COLUMN_PH_PUSH_KEY + ") as unread_count from " + TABLE_ALARM_HISTORY
                        + " where "
                        + COLUMN_PH_IS_READ_OK + " <> 'Y'";
                cursor = readableDBAdapter.executeQuery(query);
                if (cursor != null && cursor.moveToFirst()) {
                    unReadCount = cursor.getInt(0);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return unReadCount;
        } finally {
            close(cursor);
        }
        return unReadCount;
    }

    public int getAlarmTotalCount(){
        Cursor cursor = null;
        int totalCount = 0;
        try {
            if (open()) {
                String query = "select count(" + COLUMN_PH_PUSH_KEY + ") as total_count from " + TABLE_ALARM_HISTORY;
                cursor = readableDBAdapter.executeQuery(query);
                if (cursor != null && cursor.moveToFirst()) {
                    totalCount = cursor.getInt(0);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return totalCount;
        } finally {
            close(cursor);
        }
        return totalCount;
    }

    public int getAlarmTotalCount(String pushTypeArgs){
        Cursor cursor = null;
        int totalCount = 0;
        try {
            if (open()) {
                String query = "select count(" + COLUMN_PH_PUSH_KEY + ") as total_count from " + TABLE_ALARM_HISTORY;
                if(TextUtils.isEmpty(pushTypeArgs) == false){
                    query = " where " + COLUMN_PH_ALARM_CODE + " IN ("+pushTypeArgs+")";
                }
                cursor = readableDBAdapter.executeQuery(query);
                if (cursor != null && cursor.moveToFirst()) {
                    totalCount = cursor.getInt(0);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return totalCount;
        } finally {
            close(cursor);
        }
        return totalCount;
    }

    public int getAlarmCount(String pushId){
        Cursor cursor = null;
        int totalCount = 0;
        try {
            if (open()) {

                String query = "select count(" + COLUMN_PH_PUSH_KEY + ") as alarm_count from "
                        + TABLE_ALARM_HISTORY
                        + " WHERE " + COLUMN_PH_PUSH_KEY + "='"
                        + pushId + "'";
                cursor = readableDBAdapter.executeQuery(query);
                if (cursor != null && cursor.moveToFirst()) {
                    totalCount = cursor.getInt(0);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return totalCount;
        } finally {
            close(cursor);
        }
        return totalCount;
    }

    public long insertAlarmHistory(AlarmVo vo) {
        if(getAlarmCount(vo.getPushKey()) > 0) {
            return 0;
        }
        long rtn = 0;
        try {
            writableDBAdapter.startTransaction();
            ContentValues insertVlueset = new ContentValues();
            insertVlueset.put(COLUMN_PH_PUSH_KEY, vo.getPushKey());
            insertVlueset.put(COLUMN_PH_ALARM_CODE, vo.getAlarmCode());
            insertVlueset.put(COLUMN_PH_TITLE, vo.getTitle());
            insertVlueset.put(COLUMN_PH_ALARM_MESSAGE, vo.getAlarmMessage());
            insertVlueset.put(COLUMN_PH_OCCUR_TIME, vo.getOccurTime());
            insertVlueset.put(COLUMN_PH_RECEIVE_DATE, System.currentTimeMillis());
            insertVlueset.put(COLUMN_PH_IS_READ_OK, vo.getIsReadOk());
            insertVlueset.put(COLUMN_PH_IS_CLICK_OK, vo.getIsClickOk());

            rtn = writableDBAdapter.insert(TABLE_ALARM_HISTORY, null, insertVlueset);
            writableDBAdapter.commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            writableDBAdapter.endTransaction();
        }
        return rtn;
    }

    public AlarmVo getRecentAlarmVo(){
        Cursor cursor = null;
        AlarmVo vo = null;
        try {
            if (open()) {
                cursor = readableDBAdapter.executeQuery(
                        TABLE_ALARM_HISTORY,
                        alarm_history_column,
                        null,
                        null,
                        null,
                        null,
                        COLUMN_PH_RECEIVE_DATE + " desc" + " LIMIT " + 1
                );
                if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
                    vo = getPushMessageFromCursor(cursor);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return vo;
        } finally {
            close(cursor);
        }
        return vo;
    }
    public int deleteAlarmHistory(String[] ids) {
        int result = 0;
        try {
            writableDBAdapter.startTransaction();

            result = writableDBAdapter.delete(TABLE_ALARM_HISTORY, COLUMN_PH_IDX + " IN (" +
                    TextUtils.join(",", Collections.nCopies(ids.length, "?")) +
                    ")", ids);
            writableDBAdapter.commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        } finally {
            writableDBAdapter.endTransaction();
        }
        return result;
    }
    public int deleteAlarmHistory(long id) {
        int result = 0;
        try {
            writableDBAdapter.startTransaction();
            result = writableDBAdapter.delete(TABLE_ALARM_HISTORY, COLUMN_PH_IDX + "=?", new String[]{String.valueOf(id)});
            writableDBAdapter.commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        } finally {
            writableDBAdapter.endTransaction();
        }
        return result;
    }
    public int deleteAlarmHistory(String pushId) {
        int result = 0;
        try {
            writableDBAdapter.startTransaction();
            result = writableDBAdapter.delete(TABLE_ALARM_HISTORY, COLUMN_PH_PUSH_KEY + "=?", new String[]{pushId});
            writableDBAdapter.commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        } finally {
            writableDBAdapter.endTransaction();
        }
        return result;
    }
    public int deleteAlarmHistoryAll() {
        int result = 0;
        try {
            writableDBAdapter.startTransaction();
            result = writableDBAdapter.delete(
                    TABLE_ALARM_HISTORY,
                    null, null
            );
            writableDBAdapter.commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        } finally {
            writableDBAdapter.endTransaction();
        }
        return result;
    }

    private AlarmVo getPushMessageFromCursor(Cursor cursor) throws SQLException{
        AlarmVo vo = new AlarmVo();
        vo.setIdx(cursor.getLong(cursor.getColumnIndex(COLUMN_PH_IDX)));
        vo.setPushKey(cursor.getString(cursor.getColumnIndex(COLUMN_PH_PUSH_KEY)));
        vo.setAlarmCode(cursor.getString(cursor.getColumnIndex(COLUMN_PH_ALARM_CODE)));
        vo.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_PH_TITLE)));
        vo.setAlarmMessage(cursor.getString(cursor.getColumnIndex(COLUMN_PH_ALARM_MESSAGE)));
        vo.setReceiveDate(cursor.getLong(cursor.getColumnIndex(COLUMN_PH_RECEIVE_DATE)));
        vo.setOccurTime(cursor.getString(cursor.getColumnIndex(COLUMN_PH_OCCUR_TIME)));
        vo.setIsReadOk(cursor.getString(cursor.getColumnIndex(COLUMN_PH_IS_READ_OK)));
        vo.setIsClickOk(cursor.getString(cursor.getColumnIndex(COLUMN_PH_IS_CLICK_OK)));
        return vo;
    }

}
