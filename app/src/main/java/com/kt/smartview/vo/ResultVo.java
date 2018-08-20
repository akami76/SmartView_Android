package com.kt.smartview.vo;

import com.google.gson.annotations.SerializedName;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class ResultVo<T> {
    @SerializedName("errorCode")
    private String errorCode;
    @SerializedName("errorDescription")
    private String errorDescription;
    @SerializedName("data")
    private Object data;

    public T t;
    public ResultVo() {
    }

    public ResultVo(String errorCode, String errorDescription) {
        this(errorCode, errorDescription, null);
    }

    public ResultVo(String code, String message, Object data) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.data = data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
