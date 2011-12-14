package com.minder.app.tf2backpack;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataBaseHelper extends SQLiteOpenHelper {
	private static final int DB_VERSION = 4;

    private static String DB_NAME = "items";

    private SQLiteDatabase myDataBase; 
    private final Context myContext;
    
    private static final String DICTIONARY_TABLE_CREATE_ITEMS = "CREATE TABLE items (_id INTEGER PRIMARY KEY, name TEXT, defindex NUMERIC, quality NUMERIC, type_name TEXT, description TEXT, proper_name NUMERIC, attributes TEXT);";
    private static final String DICTIONARY_TABLE_CREATE_ATTRIBUTES = "CREATE TABLE attributes (_id INTEGER PRIMARY KEY, name TEXT, defindex NUMERIC, description_string TEXT, description_format NUMERIC, effect_type NUMERIC, hidden NUMERIC);";
    private static final String DICTIONARY_TABLE_CREATE_ITEM_ATTRIBUTES = "CREATE TABLE item_attributes (_id INTEGER PRIMARY KEY, itemdefindex NUMERIC, attributedefindex NUMERIC, value REAL);";
    private static final String DICTIONARY_TABLE_CREATE_NAME_HISTORY = "CREATE TABLE name_history (_id INTEGER PRIMARY KEY, name TEXT);";
    
    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public DataBaseHelper(Context context) {
    	super(context, DB_NAME, null, DB_VERSION);
        this.myContext = context;
    }	

	@Override
	public void onCreate(SQLiteDatabase db) {
		// create the database with our two tables
		db.execSQL(DICTIONARY_TABLE_CREATE_ITEMS);
		db.execSQL(DICTIONARY_TABLE_CREATE_ATTRIBUTES);
		db.execSQL(DICTIONARY_TABLE_CREATE_ITEM_ATTRIBUTES);
		db.execSQL(DICTIONARY_TABLE_CREATE_NAME_HISTORY);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(Util.GetTag(), "Updating db from " + oldVersion + " to " + newVersion);
		if (oldVersion <= 1){
			db.execSQL("ALTER TABLE items ADD quality NUMERIC");
			// update quality levels for all items
			db.execSQL("UPDATE items SET quality=6 WHERE defindex>30");
			db.execSQL("UPDATE items SET quality=5 WHERE defindex=266");
			db.execSQL("UPDATE items SET quality=5 WHERE defindex=267");
		}
		if (oldVersion <= 2) {
			db.execSQL(DICTIONARY_TABLE_CREATE_ITEM_ATTRIBUTES);
			db.execSQL("DROP TABLE items");
			db.execSQL(DICTIONARY_TABLE_CREATE_ITEMS);
		}
		if (oldVersion <= 3) {
			db.execSQL(DICTIONARY_TABLE_CREATE_NAME_HISTORY);
		}
	}


}
