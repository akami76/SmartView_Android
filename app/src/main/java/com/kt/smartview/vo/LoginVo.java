package com.kt.smartview.vo;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class LoginVo {
    private long timeMills;
    private String userId;
    private String password;
    private String otpCode;
    private String deviceKey;
    private boolean isSaveId;

    public long getTimeMills() {
        return timeMills;
    }

    public void setTimeMills(long timeMills) {
        this.timeMills = timeMills;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public String getDeviceKey() {
        return deviceKey;
    }

    public void setDeviceKey(String deviceKey) {
        this.deviceKey = deviceKey;
    }

    public boolean isSaveId() {
        return isSaveId;
    }

    public void setSaveId(boolean saveId) {
        isSaveId = saveId;
    }

    public LoginVo(long timeMills, String userId, String password, String otpCode, String deviceKey, boolean isSaveId) {
        this.timeMills = timeMills;
        this.userId = userId;
        this.password = password;
        this.otpCode = otpCode;
        this.deviceKey = deviceKey;
        this.isSaveId = isSaveId;
    }
}
