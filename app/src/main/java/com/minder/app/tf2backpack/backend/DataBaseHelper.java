package com.minder.app.tf2backpack.backend;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.Util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataBaseHelper extends SQLiteOpenHelper {
	private static final int DB_VERSION = 10;

    private static String DB_NAME = "items";

    private SQLiteDatabase myDataBase; 
    private final Context myContext;
    
    private static final String DICTIONARY_TABLE_CREATE_ITEMS = "CREATE TABLE items (_id INTEGER PRIMARY KEY, name TEXT, defindex NUMERIC, item_slot TEXT, quality NUMERIC, type_name TEXT, description TEXT, proper_name NUMERIC);";
    private static final String DICTIONARY_TABLE_CREATE_ATTRIBUTES = "CREATE TABLE attributes (_id INTEGER PRIMARY KEY, name TEXT, defindex NUMERIC, description_string TEXT, description_format NUMERIC, effect_type NUMERIC, hidden NUMERIC);";
    private static final String DICTIONARY_TABLE_CREATE_ITEM_ATTRIBUTES = "CREATE TABLE item_attributes (_id INTEGER PRIMARY KEY, itemdefindex NUMERIC, attributedefindex NUMERIC, value REAL);";
    private static final String DICTIONARY_TABLE_CREATE_NAME_HISTORY = "CREATE TABLE name_history (_id INTEGER PRIMARY KEY, name TEXT);";
    private static final String DICTIONARY_TABLE_CREATE_ID_CACHE = "CREATE TABLE steamid_cache (steamid INTEGER PRIMARY KEY, name TEXT);";
    private static final String DICTIONARY_TABLE_CREATE_PARTICLES = "CREATE TABLE particles (id INTEGER PRIMARY KEY, name TEXT);";
    private static final String DICTIONARY_TABLE_CREATE_STRANGE_SCORE_TYPES = "CREATE TABLE strange_score_types (type INTEGER PRIMARY KEY, type_name TEXT, level_data TEXT);";
    private static final String DICTIONARY_TABLE_CREATE_STRANGE_ITEM_LEVEL_TYPES = "CREATE TABLE strange_item_levels (id INTEGER PRIMARY KEY, type_name TEXT);";
    private static final String DICTIONARY_TABLE_CREATE_QUALITIES = "CREATE TABLE item_qualities (id INTEGER PRIMARY KEY, name TEXT);";
    
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
		db.execSQL(DICTIONARY_TABLE_CREATE_ID_CACHE);
		db.execSQL(DICTIONARY_TABLE_CREATE_PARTICLES);
		db.execSQL(DICTIONARY_TABLE_CREATE_STRANGE_SCORE_TYPES);
		db.execSQL(DICTIONARY_TABLE_CREATE_STRANGE_ITEM_LEVEL_TYPES);
		db.execSQL(DICTIONARY_TABLE_CREATE_QUALITIES);
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
		if (oldVersion <= 4) {
			db.execSQL(DICTIONARY_TABLE_CREATE_ID_CACHE);
		}
		if (oldVersion <= 5) {
			db.execSQL("ALTER TABLE items ADD item_slot TEXT");
		}
		if (oldVersion <= 8) {
			db.execSQL(DICTIONARY_TABLE_CREATE_PARTICLES);
			db.execSQL(DICTIONARY_TABLE_CREATE_STRANGE_SCORE_TYPES);
			db.execSQL(DICTIONARY_TABLE_CREATE_STRANGE_ITEM_LEVEL_TYPES);
		}
		if (oldVersion <= 9) {
			db.execSQL(DICTIONARY_TABLE_CREATE_QUALITIES);
			
			// Fill missing data so users don't have to reload schema files
			// for seemingly no reason at all
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('0', 'Normal')");
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('1', 'Genuine')");
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('2', 'rarity2')");
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('3', 'Vintage')");
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('4', 'rarity3')");
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('5', 'Unusual')");
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('6', 'Unique')");
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('7', 'Community')");
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('8', 'Valve')");
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('9', 'Self-Made')");
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('10', 'Customized')");
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('11', 'Strange')");
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('12', 'Completed')");
			db.execSQL("INSERT INTO item_qualities (id, name) VALUES ('13', 'Haunted')");
		}
	}

	public static String getSteamUserName(SQLiteDatabase db, long steamId) {
        Cursor c = db.rawQuery("SELECT * FROM steamid_cache WHERE steamid=?", new String[] { String.valueOf(steamId) });
        String name = "";
        
        if (c != null) {
        	if (c.getCount() > 0) {
	        	c.moveToFirst();
	        	name = c.getString(1);
        	}
        	c.close();
        }
		
		return name;
	}
	
	public static void cacheSteamUserName(long steamid, String name) {
		// TODO could probably remove this null check...
		if (name != null) {
			App.getDataManager().getDatabaseHandler().execSql("REPLACE INTO steamid_cache (steamid, name) VALUES ('" + steamid + "', '" + name.replace("'", "''") + "');");
		}
	}
}
