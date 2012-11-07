package com.minder.app.tf2backpack.backend;

import com.minder.app.tf2backpack.backend.DataManager.Request;

public interface AsyncTaskListener {
	public void onPreExecute();
	public void onProgressUpdate(ProgressUpdate object);
	public void onPostExecute(Request object);
	//public void onCancelled(Object object);
}
