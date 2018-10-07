package com.minder.app.tf2backpack.backend;

import com.minder.app.tf2backpack.backend.DataManager.Request;

public interface AsyncTaskListener {
	void onPreExecute();
	void onProgressUpdate(ProgressUpdate object);
	void onPostExecute(Request object);
	//public void onCancelled(Object object);
}
