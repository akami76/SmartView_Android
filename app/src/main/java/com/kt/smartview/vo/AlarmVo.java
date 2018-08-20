package com.kt.smartview.vo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */

public class AlarmVo implements Parcelable {
    private long idx;
    private String pushKey;
    private String alarmCode;
    private String title;
    private String alarmMessage;
    private String occurTime;
    private long receiveDate;
    private String isReadOk;
    private String isClickOk;

    public AlarmVo(long idx, String pushKey, String alarmCode, String title, String alarmMessage, String occurTime, long receiveDate, String isReadOk, String isClickOk) {
        this.idx = idx;
        this.pushKey = pushKey;
        this.alarmCode = alarmCode;
        this.title = title;
        this.alarmMessage = alarmMessage;
        this.occurTime = occurTime;
        this.receiveDate = receiveDate;
        this.isReadOk = isReadOk;
        this.isClickOk = isClickOk;
    }

    public long getIdx() {
        return idx;
    }

    public void setIdx(long idx) {
        this.idx = idx;
    }

    public String getPushKey() {
        return pushKey;
    }

    public void setPushKey(String pushKey) {
        this.pushKey = pushKey;
    }

    public String getAlarmCode() {
        return alarmCode;
    }

    public void setAlarmCode(String alarmCode) {
        this.alarmCode = alarmCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlarmMessage() {
        return alarmMessage;
    }

    public void setAlarmMessage(String alarmMessage) {
        this.alarmMessage = alarmMessage;
    }

    public String getOccurTime() {
        return occurTime;
    }

    public void setOccurTime(String occurTime) {
        this.occurTime = occurTime;
    }

    public long getReceiveDate() {
        return receiveDate;
    }

    public void setReceiveDate(long receiveDate) {
        this.receiveDate = receiveDate;
    }

    public String getIsReadOk() {
        return isReadOk;
    }

    public void setIsReadOk(String isReadOk) {
        this.isReadOk = isReadOk;
    }

    public String getIsClickOk() {
        return isClickOk;
    }

    public void setIsClickOk(String isClickOk) {
        this.isClickOk = isClickOk;
    }

    public static final String EXT_ALARMVO = "EXT_ALARMVO";

    public AlarmVo() {
        super();
    }


    public AlarmVo(Parcel in) {
        idx = in.readLong();
        pushKey = in.readString();
        alarmCode = in.readString();
        title = in.readString();
        alarmMessage = in.readString();
        occurTime = in.readString();
        receiveDate = in.readLong();
        isReadOk = in.readString();
        isClickOk = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(idx);
        dest.writeString(pushKey);
        dest.writeString(alarmCode);
        dest.writeString(title);
        dest.writeString(alarmMessage);
        dest.writeString(occurTime);
        dest.writeLong(receiveDate);
        dest.writeString(isReadOk);
        dest.writeString(isClickOk);
    }

    public static final Parcelable.Creator<AlarmVo> CREATOR = new Parcelable.Creator<AlarmVo>() {
        public AlarmVo createFromParcel(Parcel in) {
            return new AlarmVo(in);
        }

        public AlarmVo[] newArray(int size) {
            return new AlarmVo[size];
        }
    };
}
