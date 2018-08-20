package com.kt.smartview.utils;

public class YWMTools {

        /**
         * 문자열이 null 또는 공백일 경우 default 값을 리턴 한다.
         * @param sValue
         * @param sDefaultValue
         * @return String
         */
        public static String getString(String sValue, String sDefaultValue) {

            if (sValue == null || sValue.trim().length() == 0)
                return sDefaultValue;
            if("null".equals(sValue.toLowerCase())){
                return sDefaultValue;
            }
            return sValue;
        }

        /**
         * 문자열이 null 또는 공백일 경우 default 값을 리턴 한다.
         * @param obj
         * @param sDefaultValue
         * @return String
         */
        public static String getString(Object obj, String sDefaultValue) {
            String sValue = String.valueOf(obj);
            if (sValue == null || sValue.trim().length() == 0)
                return sDefaultValue;
            if("null".equals(sValue.toLowerCase())){
                return sDefaultValue;
            }
            return sValue;
        }

        /**
         * 특정 길이만큼 문자열을 잘라 낸다.flag 값에 따라 문자열의 끝에 "..."을 추가 한다.
         * @param sValue
         * @param sDefaultValue
         * @param len
         * @param flag
         * @return String
         */
        public static String getString(String sValue, String sDefaultValue, int len) {
            return YWMTools.getString(sValue, sDefaultValue, len, false);
        }

        /**
         * 특정 길이만큼 문자열을 잘라 낸다.flag 값에 따라 문자열의 끝에 "..."을 추가 한다.
         * @Method Name  : getString
         * @param sValue
         * @param sDefaultValue
         * @param len
         * @param flag
         * @return String
         */
        public static String getString(String sValue, String sDefaultValue, int len, boolean flag) {

            if (sValue == null || sValue.trim().length() == 0)
                return sDefaultValue;

            if (sValue.length() > len)
                return sValue.substring(0, len) + (flag ? " ..." : "");

            return sValue;
        }

        /**
         * 숫자에 3개 마다 콤마(,)를 추가 한다.
         * @param value
         * @return String
         */
        public static String addComma(long value) {
            java.text.DecimalFormat df = new java.text.DecimalFormat("###,###");
            return df.format(value);
        }

        public static boolean isYn2Bool(String str){
            if(str == null) return false;
            if("y".equals(str.toLowerCase())){
                return true;
            }
            return false;
        }
    }