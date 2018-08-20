package com.kt.smartview.db;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.kt.smartview.GlobalApplication;
import com.kt.smartview.support.log.YWMLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class DomaDBHelper extends SQLiteOpenHelper {
    private final YWMLog logger = new YWMLog(DomaDBHelper.class);
    private static final String DB_ASSET_PATH = "database/";
    private static final String DB_NAME = "smart_view.sqlite";
    private static final String DB_PATH = "/data/data/com.kt.smartview/databases/";
    private final Context mContext;
    private static DomaDBHelper dbHelper;
    public static DomaDBHelper getInstance(final Context context){
        if(dbHelper == null){
            dbHelper = new DomaDBHelper(context);
        }
        return dbHelper;
    }
    private DomaDBHelper(final Context context) {
        super(context, DB_NAME, null, GlobalApplication.DATABASE_VERSION);

        mContext = context;
    }
    public static boolean isExistDBFile(){
        try {
            String myPath = DB_PATH + DB_NAME;
            File fileTest = new File(myPath);
            boolean exists = fileTest.exists();
            return exists;

        } catch (Exception e) {
            return false;
        }
    }
    public static boolean checkDatabase() {
        if(isExistDBFile() == false) return false;
        SQLiteDatabase checkDB = null;
        try {
            String myPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath, null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);

        } catch (SQLiteException e) {
            return false;
        } finally {
            if (checkDB != null) {
                checkDB.close();
            }
        }
        return checkDB != null ? true : false;
    }

    public boolean createDatabase(boolean isDelete){
        boolean isCreated = false;
        try {
            boolean dbExist = checkDatabase();
            if (dbExist) {
                if(isDelete){
                    deleteDatabase();
                    this.getReadableDatabase();
                    isCreated = copyDatabase();
                }
            } else {
                this.getReadableDatabase();
                isCreated = copyDatabase();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return isCreated;
    }
    private void deleteDatabase() throws IOException {
        File fileTest = mContext.getFileStreamPath(DB_NAME);
        boolean exists = fileTest.exists();
        if (exists) {
            boolean isSuccess = fileTest.delete();
            Log.e("TEST", ("delete->" +isSuccess));
        } else {
            Log.e("TEST", "not exists");
        }
    }

    public static void DeleteDatabase() {
        File f = new File(DB_PATH + DB_NAME);
        f.deleteOnExit();
    }


    private boolean copyDatabase() throws IOException {
        File fileTest = mContext.getFileStreamPath(DB_NAME);
        boolean exists = fileTest.exists();
        if (!exists) {
            logger.d("not exists");
            OutputStream databaseOutputStream = new FileOutputStream(DB_PATH
                    + DB_NAME);
            InputStream databaseInputStream = mContext.getAssets().open("database/" + DB_NAME);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = databaseInputStream.read(buffer)) > 0) {
                databaseOutputStream.write(buffer);
            }
            /*
             * databaseInputStream.close(); databaseInputStream =
			 * myContext.getAssets().open(DBpart2); while ((length =
			 * databaseInputStream.read(buffer)) > 0) {
			 * databaseOutputStream.write(buffer); }
			 */
            // Close the streams
            databaseInputStream.close();
            databaseOutputStream.flush();
            databaseOutputStream.close();
            return true;
        } else {
            logger.d("exists");
            return false;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        logger.d("DBHelper onCreate...");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        logger.d("DBHelper onUpgrade");

    }
    private void execSql(SQLiteDatabase db, String query){
        try {
            db.execSQL(query);
        } catch (SQLException e) {
            logger.d(query + " --> exec failed...", e);
        }
    }
}
