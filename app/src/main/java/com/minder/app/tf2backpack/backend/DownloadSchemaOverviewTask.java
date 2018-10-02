package com.minder.app.tf2backpack.backend;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.minder.app.tf2backpack.ApiKey;
import com.minder.app.tf2backpack.Util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Date;

/**
 * Created by Patrik on 2018-09-15.
 */

public class DownloadSchemaOverviewTask extends AsyncTask<Void, Float, Void> {

    public interface ProgressListener {
        void onProgress(float t);
        void onComplete(Date dataLastModified);
        void onError(Exception error);
    }

    private static final float OVERVIEW_PERCENT = 0.3f;
    private static final float ITEMS_PERCENT = 1.0f - OVERVIEW_PERCENT;

    private Context context;
    private ProgressListener listener;

    public DownloadSchemaOverviewTask(Context context, ProgressListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void downloadParseAndSaveSchemaFilesToDb(SQLiteDatabase sqlDb, int start, final int parsedItems) throws Exception {
		HttpConnection connection =
				HttpConnection.string("http://api.steampowered.com/IEconItems_440/GetSchemaItems/v1/?key=" +
                        ApiKey.get() + "&format=json&language=en" + (start >= 0 ? "&start=" + start : ""));
        
        // Function with limit at 1 and will be about 90% on the way when we have parsed 5000 items
        // which is the close to the total amount of items in the game at the time of writing.
        // This ensures we do not surpass 1 if more items are added and the app is not changed.
        float itemsProgress = (float)(1 - Math.pow(Math.E, -parsedItems / 2500.0));
        // We get 1000 more items in each call except the final one.
        float nextItemsProgress =  (float)(1 - Math.pow(Math.E, -(parsedItems + 1000) / 2500.0));

        final float totalProgressSoFar = OVERVIEW_PERCENT + itemsProgress * ITEMS_PERCENT;
        final float ourSegmentOfTotal = ITEMS_PERCENT * (nextItemsProgress - itemsProgress);

        Log.d(Util.GetTag(), "parsedItems: " + parsedItems + ", itemsProgress: " + itemsProgress +
                ", nextItemsProgress: " + nextItemsProgress + ", totalProgressSoFar: " + totalProgressSoFar + ", ourSegmentOfTotal: " + ourSegmentOfTotal);

		InputStream inputStream = connection.executeStream(new HttpConnection.DownloadProgressListener() {
            private int totalSize;

            public void totalSize(long totalSize) {
                this.totalSize = (int) totalSize;
            }

            public void progressUpdate(long currentSize) {
                if (totalSize < 0) return;
                if (currentSize == -1) {
                    // Do nothing
                }
                else {
                    float percent = Math.min(1, currentSize / totalSize * ourSegmentOfTotal);
                    Log.d(Util.GetTag(), "http progress: " + percent + ", parsedItems: " + parsedItems);
                    publishProgress(totalProgressSoFar + percent);
                }
            }
        });

		if (inputStream != null) {
			GameSchemeParser gs;
			try {
				gs = new GameSchemeParser(inputStream, sqlDb);
				publishProgress(totalProgressSoFar + ourSegmentOfTotal);
			} finally {
				try {
					inputStream.close();
				} catch (IOException e) {}
			}
			if (gs.getNextStart() >= 0) {
				downloadParseAndSaveSchemaFilesToDb(sqlDb, gs.getNextStart(), parsedItems + gs.getParsedItems());
			}
		}
		else {
			throw connection.getException();
		}
	}

    @Override
    protected Void doInBackground(Void... voids) {
        HttpConnection connection;
        try {
            connection = HttpConnection.string("http://api.steampowered.com/IEconItems_440/GetSchemaOverview/v1/?key=" +
                    ApiKey.get() + "&format=json&language=en");
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        final InputStream inputStream = connection.executeStream(new HttpConnection.DownloadProgressListener() {
            private int totalSize = -1;

            public void totalSize(long totalSize) {
                this.totalSize = (int) totalSize;
            }

            public void progressUpdate(long currentSize) {
                if (totalSize < 0) return;
                if (currentSize == -1)
                    publishProgress(0.0f);
                else {
                    // Calculate percent of whole, make room for parcing by taking of 5 percent points
                    // from the overview percent point.
                    float percent = currentSize / totalSize * (OVERVIEW_PERCENT - 0.05f);
                    Log.d(Util.GetTag(), "overview percent: " + percent + ", totalSize: " + totalSize + ", currentSize: " + currentSize);
                    publishProgress(percent);
                }
            }
        });

        if (inputStream != null) {
            Log.i(Util.GetTag(), "Saving to database...");
            long start = System.currentTimeMillis();

            DataBaseHelper.runWithWritableDb(context, new DataBaseHelper.RunWithWritableDb() {
                public void run(SQLiteDatabase sqlDb) {
                    sqlDb.beginTransaction();
                    try {
                        // Cleanup old data from db
                        {
                            // removes everything from database, else we would get duplicates
                            sqlDb.delete("items", null, null);
                            sqlDb.delete("item_attributes", null, null);
                            sqlDb.delete("attributes", null, null);
                            sqlDb.delete("item_attributes", null, null);
                            sqlDb.delete("particles", null, null);
                            sqlDb.delete("strange_score_types", null, null);
                            sqlDb.delete("item_qualities", null, null);

                            // we need to fetch table names
                            final Cursor c = sqlDb.rawQuery("SELECT type_name FROM strange_item_levels", null);
                            while (c.moveToNext()) {
                                // delete each table for item levels
                                sqlDb.execSQL("DROP TABLE " + c.getString(0));
                            }
                            c.close();

                            sqlDb.delete("strange_item_levels", null, null);
                        }

                        new GameSchemaOverviewParser(inputStream, sqlDb);
                        try {
                            inputStream.close();
                        } catch (IOException e) { }
                        
                        publishProgress(OVERVIEW_PERCENT);
                        downloadParseAndSaveSchemaFilesToDb(sqlDb, -1, 0);
                        sqlDb.setTransactionSuccessful();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    } finally {
                        try {
                            inputStream.close();
                        } catch (IOException e) { }
                        sqlDb.endTransaction();
                    }
                    
                }
            });

            publishProgress(1.0f);
            Log.i(Util.GetTag(), "Save to database finished - Time: " + (System.currentTimeMillis() - start) + " ms");
        }
        else {
            connection.getException().printStackTrace();
        }

        return null;
    }

    @Override
	protected void onProgressUpdate(Float... progress) {
        Log.d(Util.GetTag(), "Progress: " + progress[0]);
		listener.onProgress(progress[0]);
	}

    @Override
    protected void onPostExecute(Void aVoid) {
        listener.onComplete(new Date());
    }
}
