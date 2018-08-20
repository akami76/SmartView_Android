package com.kt.smartview.vo;

/**
 * Created by jkjeon on 2016-12-26.
 */

public class OTPResponseVo {
    private String encodedOtpCode;
    private int otpStandBySec;

    public String getEncodedOtpCode() {
        return encodedOtpCode;
    }

    public void setEncodedOtpCode(String encodedOtpCode) {
        this.encodedOtpCode = encodedOtpCode;
    }

    public int getOtpStandBySec() {
        return otpStandBySec;
    }

    public void setOtpStandBySec(int otpStandBySec) {
        this.otpStandBySec = otpStandBySec;
    }
}
