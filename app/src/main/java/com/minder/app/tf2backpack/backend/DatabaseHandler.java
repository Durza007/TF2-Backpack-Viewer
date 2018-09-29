package com.minder.app.tf2backpack.backend;

import java.util.LinkedList;
import java.util.Queue;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.minder.app.tf2backpack.Util;

public class DatabaseHandler implements Runnable {
	private static class Query {
		final String sql;
		final Object[] args;

		public Query(String sql, Object[] args) {
			this.sql = sql;
			this.args = args;
		}
	}

	private DataBaseHelper db;
	private Thread sqlThread;

	private Context context;
	private Queue<Query> sqlQueryQueue;
	private final Object mLock;
	private boolean mRunning;
	
	public DatabaseHandler(Context context){
		this.context = context;
		this.db = new DataBaseHelper(context);
		sqlQueryQueue = new LinkedList<Query>();
		mLock = new Object();
		mRunning = true;
	}
	
	public void execSql(String sql, Object[] args){
		if (sqlThread == null){
			sqlThread = new Thread(this);
			sqlThread.setDaemon(true);
			sqlThread.setName("dbThread");
			sqlThread.start();
		}
		
		synchronized (mLock){
			sqlQueryQueue.add(new Query(sql, args));
			mLock.notify();
		}
	}
	
	public SQLiteDatabase getReadableDatabase(){
		return db.getReadableDatabase();
	}
	
	public void run() {
        while (mRunning)
        {
            Query query;
            // lock queue for thread safe work
            synchronized (mLock)
            {
                // if no messages available - wait
                while (sqlQueryQueue.isEmpty())
                {
					try {
						mLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
                }
				query = sqlQueryQueue.poll();
            }

            if (query != null)
            {
            	final Query finalQuery = query;
				DataBaseHelper.runWithWritableDb(context, new DataBaseHelper.RunWithWritableDb() {
					public void run(SQLiteDatabase db) {
						db.execSQL(finalQuery.sql, finalQuery.args);
					}
				});
            }
        }
	}

}
