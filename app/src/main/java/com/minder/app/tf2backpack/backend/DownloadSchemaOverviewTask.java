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

/**
 * Created by Patrik on 2018-09-15.
 */

public class DownloadSchemaOverviewTask extends AsyncTask<Void, ProgressUpdate, Void> {

    private Context context;

    public DownloadSchemaOverviewTask(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        HttpConnection connection =
                HttpConnection.string("http://api.steampowered.com/IEconItems_440/GetSchemaOverview/v1/?key=" +
                        ApiKey.get() + "&format=json&language=en");

        final InputStream inputStream = connection.executeStream(new HttpConnection.DownloadProgressListener() {
            private int totalSize;

            public void totalSize(long totalSize) {
                this.totalSize = (int) totalSize;
                publishProgress(new ProgressUpdate(DataManager.PROGRESS_DOWNLOADING_SCHEMA_UPDATE, this.totalSize, 0));
            }

            public void progressUpdate(long currentSize) {
                if (currentSize == -1)
                    publishProgress(new ProgressUpdate(DataManager.PROGRESS_PARSING_SCHEMA, 0, 0));
                else
                    publishProgress(new ProgressUpdate(DataManager.PROGRESS_DOWNLOADING_SCHEMA_UPDATE, totalSize, (int) currentSize));
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
                        DownloadSchemaFilesTask.downloadParseAndSaveSchemaFilesToDb(sqlDb, null, -1);
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

            Log.i(Util.GetTag(), "Save to database finished - Time: " + (System.currentTimeMillis() - start) + " ms");
        }
        else {
            connection.getException().printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        DataManager.saveGameSchemeDownloaded();
    }
}
