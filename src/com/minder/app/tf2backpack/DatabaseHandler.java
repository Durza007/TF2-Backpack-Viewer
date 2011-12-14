package com.minder.app.tf2backpack;

import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DatabaseHandler implements Runnable{
	private DataBaseHelper db;
	private SQLiteDatabase sqlDb;
	
	private Thread sqlThread;
	
	private Queue<String> sqlQueryQueue;
	private Object mLock;
	private boolean mRunning;
	private long start;
	
	public DatabaseHandler(Context context){
		db = new DataBaseHelper(context);
		sqlQueryQueue = new LinkedList<String>();
		mLock = new Object();
		mRunning = true;
	}
	
	public void ExecSql(String sql){
		if (sqlThread == null){
			sqlThread = new Thread(this);
			sqlThread.setDaemon(true);
			sqlThread.setName("dbThread");
			sqlThread.start();
			start = System.currentTimeMillis();
		}
		
		synchronized (mLock){
			sqlQueryQueue.add(sql);
			mLock.notify();
		}
	}
	
	public SQLiteDatabase getReadableDatabase(){
		return db.getReadableDatabase();
	}
	
	public void run() {
        while (mRunning)
        {
            String sql;
            // lock queue for thread safe work
            synchronized (mLock)
            {
                // if no messages available - wait
                while (sqlQueryQueue.isEmpty())
                {
                	Log.i(Util.GetTag(), "Empty queue - Time: " + (System.currentTimeMillis() - start));
					try {
						mLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
                }
                sql = sqlQueryQueue.poll();
            }

            if (sql != null)
            {
            	OpenDB();
            	sqlDb.execSQL(sql);
            }
        }
	}
	
	public void OpenDB(){
		if (sqlDb == null){
			sqlDb = db.getWritableDatabase();
		}
	}
	
	public void CloseDB(){
		if (sqlDb.isOpen()){
			sqlDb.close();
			sqlDb = null;
		}
	}

}
