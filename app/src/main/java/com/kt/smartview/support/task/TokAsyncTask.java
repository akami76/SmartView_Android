/**
 * Copyright (c) 2014  Tokque , Inc. All Rights Reserved.
 *
 * Class Name  : TokAsyncTask.java
 * Description : 
 * 
 * @author: 전재권
 * @version: 1.0
 * @since: 2015. 1. 7.
 */
package com.kt.smartview.support.task;

import android.os.AsyncTask;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class TokAsyncTask extends AsyncTask<Object, Void, AsyncTaskResult<Object>>{
	@Override
	protected AsyncTaskResult<Object> doInBackground(Object... arg0) {
		// TODO Auto-generated method stub
		return null;
		
	}
	public TokAsyncTask executeObj(Object... obj) {
        try {
            if (ExecutorFactory.executeOnExecutorMethod != null) {
            	ExecutorFactory.executeOnExecutorMethod.invoke(this, ExecutorFactory.getExecutor(), obj);
                return this;
            }
        } catch (InvocationTargetException e) {
            // fall-through
        } catch (IllegalAccessException e) {
            // fall-through
        }
        super.execute(obj);
        return this;
    }
	public TokAsyncTask execute(Map<String, Object> map) {
        return this.executeObj(map);
    }
	public TokAsyncTask execute(String json) {
		return this.executeObj(json);
    }
	public TokAsyncTask execute() {
		return this.executeObj(null);
    }
	public TokAsyncTask execute(Object obj) {
		return this.executeObj(obj);
    }
	public void stop(){
		if(getStatus() == Status.RUNNING || getStatus() != Status.FINISHED){
			cancel(true);
		}
	}
}
