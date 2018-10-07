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

/**
 * Created by Patrik on 2018-09-15.
 */

public class DownloadSchemaTask extends AsyncTask<Void, Float, DownloadSchemaTask.Result> {

    public interface ProgressListener {
        void onProgress(float t);
        void onComplete(long dataLastModified);
        void onError(Exception error);
    }

    public static class Result {
        final long lastModifiedTimestamp;
        final Exception error;

        Result(long lastModifiedTimestamp) {
            this.lastModifiedTimestamp = lastModifiedTimestamp;
            this.error = null;
        }

        Result(Exception error) {
            this.lastModifiedTimestamp = -1;
            this.error = error;
        }
    }

    private static final float OVERVIEW_PERCENT = 0.3f;
    private static final float ITEMS_PERCENT = 1.0f - OVERVIEW_PERCENT;

    private Context context;
    private ProgressListener listener;
    private long ifModifiedSinceTimestamp;

    public DownloadSchemaTask(Context context, long ifModifiedSinceTimestamp, ProgressListener listener) {
        this.context = context;
        this.listener = listener;
        this.ifModifiedSinceTimestamp = ifModifiedSinceTimestamp;
    }

    public boolean downloadParseAndSaveSchemaFilesToDb(SQLiteDatabase sqlDb, long ifModifiedSinceTimestamp, int start, final int parsedItems) throws Exception {
		HttpConnection connection =
				HttpConnection.string("http://api.steampowered.com/IEconItems_440/GetSchemaItems/v1/?key=" +
                        ApiKey.get() + "&format=json&language=en" + (start >= 0 ? "&start=" + start : ""));

        if (ifModifiedSinceTimestamp > 0) {
            connection.setIfModifiedSince(ifModifiedSinceTimestamp);
        }

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
            boolean itemsModified = true;
            try {
                itemsModified = connection.getConnection().getResponseCode() != 304;
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (itemsModified) {
                GameSchemeParser gs;
                try {
                    gs = new GameSchemeParser(inputStream, sqlDb);
                    publishProgress(totalProgressSoFar + ourSegmentOfTotal);
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                    }
                }
                if (gs.getNextStart() >= 0) {
                    return downloadParseAndSaveSchemaFilesToDb(sqlDb, -1, gs.getNextStart(), parsedItems + gs.getParsedItems());
                }
                return true;
            }
            else {
                try {
                    inputStream.close();
                } catch (IOException e) {}
                return false;
            }
		}
		else {
			throw connection.getException();
		}
	}

    @Override
    protected DownloadSchemaTask.Result doInBackground(Void... voids) {
        HttpConnection connection;
        try {
            connection = HttpConnection.string("http://api.steampowered.com/IEconItems_440/GetSchemaOverview/v1/?key=" +
                    ApiKey.get() + "&format=json&language=en");
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        if (ifModifiedSinceTimestamp > 0) {
            connection.setIfModifiedSince(ifModifiedSinceTimestamp);
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
            boolean schemaOverviewModified = true;
            try {
                schemaOverviewModified = connection.getConnection().getResponseCode() != 304;
            } catch (IOException e) {
                e.printStackTrace();
            }
            final boolean schemaOverviewModifiedFinal = schemaOverviewModified;
            long lastModified = connection.getConnection().getLastModified();
            Log.i(Util.GetTag(), "Saving to database...");
            long start = System.currentTimeMillis();

            Exception error = DataBaseHelper.runWithWritableDb(context, new DataBaseHelper.RunWithWritableDb<Exception>() {
                public Exception run(SQLiteDatabase sqlDb) {
                    sqlDb.beginTransaction();
                    try {
                        // Cleanup old data from db
                        {
                            // removes everything from database, else we would get duplicates
                            sqlDb.delete("items", null, null);
                            sqlDb.delete("item_attributes", null, null);

                            if (schemaOverviewModifiedFinal) {
                                sqlDb.delete("attributes", null, null);
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
                        }

                        if (schemaOverviewModifiedFinal) {
                            new GameSchemaOverviewParser(inputStream, sqlDb);
                        }
                        try {
                            inputStream.close();
                        } catch (IOException e) { }
                        
                        publishProgress(OVERVIEW_PERCENT);
                        // If the overView data has changed, then we force update the rest to be safe.
                        boolean hasChanges = downloadParseAndSaveSchemaFilesToDb(
                                sqlDb,
                                schemaOverviewModifiedFinal ? -1 : ifModifiedSinceTimestamp,
                                -1,
                                0);
                        if (hasChanges) {
                            sqlDb.setTransactionSuccessful();
                        }
                    } catch (Exception error) {
                        return error;
                    } finally {
                        try {
                            inputStream.close();
                        } catch (IOException e) { }
                        sqlDb.endTransaction();
                    }

                    return null;
                }
            });

            if (error == null) {
                publishProgress(1.0f);
                Log.i(Util.GetTag(), "Save to database finished - Time: " + (System.currentTimeMillis() - start) + " ms");
                return new Result(lastModified);
            }
            else {
                return new Result(error);
            }
        }
        else {
            return new Result(connection.getException());
        }
    }

    @Override
	protected void onProgressUpdate(Float... progress) {
        Log.d(Util.GetTag(), "Progress: " + progress[0]);
		listener.onProgress(progress[0]);
	}

    @Override
    protected void onPostExecute(DownloadSchemaTask.Result result) {
        if (result.error == null) {
            listener.onComplete(result.lastModifiedTimestamp);
        } else {
            listener.onError(result.error);
        }
    }
}
