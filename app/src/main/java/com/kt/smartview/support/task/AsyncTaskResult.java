package com.kt.smartview.support.task;

public class AsyncTaskResult<T> {
	private T result = null;
	private Exception exception = null;
	private YWMException ywmException = null;

	public T getResult() {
		return result;
	}

	public boolean existError() {
		boolean result = false;

		if (exception != null) {
			result = true;
		}
		if (ywmException != null) {
			result = true;
		}
		return result;
	}

	public Exception getError() {
		return exception;
	}

	public YWMException getYwmError() {
		return ywmException;
	}

	public AsyncTaskResult(T result) {
		super();
		this.result = result;
	}

	public AsyncTaskResult(Exception ex) {
		super();
		this.exception = ex;
	}

	public AsyncTaskResult(YWMException ex) {
		super();
		this.ywmException = ex;
	}

	public void setResult(T result) {
		this.result = result;
	}
	
}
