package com.kt.smartview.support.task;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class YWMException extends Exception {
	private static final long serialVersionUID = 1L;
	public int errorCode;
	public String errorMessage;
	private Exception innerException;

	public YWMException() {
		super();
	}
	public YWMException(String errorMessage) {
		this(-1, errorMessage);
	}
	public YWMException(int errorCode, String errorMessage) {
		this(errorCode, errorMessage, null);
	}
	public YWMException(int errorCode, String errorMessage,
			Exception innerException) {
		super();
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.innerException = innerException;
	}
	public YWMException(Exception innerException) {
		this(-1, null, innerException);
	}
	
	public YWMException(String errorMessage, Exception innerException) {
		this(-1, errorMessage, innerException);
	}
	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Exception getInnerException() {
		return innerException;
	}

	public void setInnerException(Exception innerException) {
		this.innerException = innerException;
	}

	@Override
	public String toString() {
		String result = super.toString();
		result += "@(ErrorCode = " + errorCode + ", ErrorMessage = "
				+ errorMessage + ')';
		if (innerException != null) {
			result += innerException.toString();
		}
		return result;
	}
}
